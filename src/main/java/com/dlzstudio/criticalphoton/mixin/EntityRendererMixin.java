package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.api.CriticalPhotonApi;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void preRender(T entity, float entityYaw, float partialTick,
                                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                              int packedLight, CallbackInfo ci) {
        if (!CriticalPhotonApi.shouldRender(entity)) {
            ci.cancel();
            return;
        }
        float alpha = CriticalPhotonApi.getRenderAlpha(entity, partialTick);
    }
}
