package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.event.ClientEventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体渲染器 Mixin - 0.3.3 视锥体修复版
 * 
 * 优化策略:
 * 1. 禁用 Minecraft 原版视锥体剔除，使用临界光子自己的判断
 * 2. 根据节流器级别跳过远处实体
 * 3. 物品实体优先剔除
 * 4. 生物实体分级剔除
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    private static boolean injected = false;

    /**
     * 注入成功标记
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        if (!injected) {
            injected = true;
            CriticalPhoton.LOGGER.info("EntityRendererMixin 注入成功！(0.3.3 视锥体修复版)");
        }
    }

    /**
     * 0.3.3 新增：禁用原版视锥体剔除
     * Minecraft 会检查实体是否在视野内，但这会阻止"视线方向但不在当前视野"的实体渲染
     * 我们返回 true 让所有实体都进入渲染流程，然后在 render() 中自己决定
     */
    @Inject(method = "shouldRenderAtSqrDistance(D)Z", at = @At("HEAD"), cancellable = true)
    public void overrideShouldRenderAtSqrDistance(double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        // 返回 true 让所有实体都进入渲染流程
        // 实际的剔除逻辑在 render() 中执行
        cir.setReturnValue(true);
        cir.cancel();
    }

    /**
     * 在实体渲染前检查是否应该渲染
     * 0.3.3 修复：使用临界光子自己的可见性判断
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void preRender(T entity, float entityYaw, float partialTick,
                                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                              int packedLight, CallbackInfo ci) {
        // 玩家实体始终渲染
        if (entity instanceof Player) {
            return;
        }

        // 载具实体始终渲染（避免玩家掉出世界）
        if (entity instanceof VehicleEntity) {
            return;
        }

        // 获取节流器级别
        int throttleLevel = 0;
        if (CriticalPhoton.getInstance() != null &&
            CriticalPhoton.getInstance().getPerformanceMonitor() != null) {
            throttleLevel = CriticalPhoton.getInstance()
                .getPerformanceMonitor()
                .getThrottler()
                .getThrottleLevel();
        }

        // 获取实体距离
        Minecraft mc = Minecraft.getInstance();
        double distSq = 0;
        if (mc.cameraEntity != null) {
            distSq = entity.distanceToSqr(mc.cameraEntity);
        }

        // 节流级别 1: 跳过 64 格外的物品
        if (throttleLevel >= 1 && entity instanceof ItemEntity) {
            if (distSq > 64 * 64) {
                ci.cancel();
                return;
            }
        }

        // 节流级别 1: 跳过 128 格外的普通生物
        if (throttleLevel >= 1 && entity instanceof Animal) {
            if (distSq > 128 * 128) {
                ci.cancel();
                return;
            }
        }

        // 节流级别 2: 跳过 96 格外的怪物
        if (throttleLevel >= 2 && entity instanceof Monster) {
            if (distSq > 96 * 96) {
                ci.cancel();
                return;
            }
        }

        // 节流级别 2: 跳过 256 格外的所有非玩家实体
        if (throttleLevel >= 2 && distSq > 256 * 256) {
            ci.cancel();
            return;
        }

        // 密度控制：每帧最多渲染 500 个实体
        if (!ClientEventSubscriber.canRenderMoreEntities()) {
            // 超出密度限制，优先跳过物品和远处生物
            if (entity instanceof ItemEntity) {
                ci.cancel();
                return;
            }
            if (distSq > 128 * 128) {
                ci.cancel();
                return;
            }
        }

        // 记录实体已渲染
        ClientEventSubscriber.recordEntityRendered();
    }
}
