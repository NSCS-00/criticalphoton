package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.renderer.AsyncRenderPipeline;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    public <T extends Entity> void preRender(T entity, double x, double y, double z,
                                              float entityYaw, float partialTick,
                                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                              int packedLight, CallbackInfo ci) {
        AsyncRenderPipeline pipeline = CriticalPhoton.getInstance().getRenderPipeline();
    }
}
