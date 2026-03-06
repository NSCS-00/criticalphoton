package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.api.CriticalPhotonApi;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.event.ClientEventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体渲染器 Mixin - 0.3.1 性能优化版
 * 
 * 优化策略:
 * 1. 距离剔除 - 超出距离的实体不渲染
 * 2. 密度剔除 - 实体密度过高时跳过部分实体
 * 3. 特殊实体优化 - 物品实体优先剔除
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
            CriticalPhoton.LOGGER.info("EntityRendererMixin 注入成功！(0.3.1 性能优化版)");
        }
    }

    /**
     * 在实体渲染前检查是否应该渲染
     * 0.3.1 新增：距离剔除、密度剔除
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

        // 1. 距离剔除 - 根据配置的距离检查
        if (!CriticalPhotonApi.shouldRender(entity)) {
            ci.cancel();
            return;
        }

        // 2. 动态密度控制 - 根据性能节流器级别调整
        int throttleLevel = 0;
        if (CriticalPhoton.getInstance() != null && 
            CriticalPhoton.getInstance().getPerformanceMonitor() != null) {
            throttleLevel = CriticalPhoton.getInstance()
                .getPerformanceMonitor()
                .getThrottler()
                .getThrottleLevel();
        }

        // 检查是否超出密度限制
        if (!ClientEventSubscriber.canRenderMoreEntities()) {
            // 超出密度限制，根据优先级决定是否跳过
            if (entity instanceof ItemEntity) {
                // 物品实体优先跳过
                ci.cancel();
                return;
            }
            // 远处实体跳过
            double distSq = getDistanceSq(entity);
            if (distSq > 256) {
                ci.cancel();
                return;
            }
        }

        // 3. 物品实体优化 - 超出一定距离的物品实体不渲染
        if (entity instanceof ItemEntity) {
            double distSq = getDistanceSq(entity);
            
            // 根据节流级别调整物品渲染距离
            int itemRenderDist = 64 - (throttleLevel * 16);
            if (distSq > itemRenderDist * itemRenderDist) {
                ci.cancel();
                return;
            }
        }

        // 记录实体已渲染
        ClientEventSubscriber.recordEntityRendered();
    }
    
    /**
     * 获取实体到摄像机的距离平方
     */
    private double getDistanceSq(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.cameraEntity != null) {
            return entity.distanceToSqr(mc.cameraEntity);
        }
        return Double.MAX_VALUE;
    }
}
