package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    
    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void preRenderLevel(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick,
                                long nanoTime, boolean renderBlockOutline,
                                net.minecraft.client.Camera camera,
                                net.minecraft.client.renderer.GameRenderer gameRenderer,
                                net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                CallbackInfo ci) {
        PerformanceMonitor monitor = CriticalPhoton.getInstance().getPerformanceMonitor();
        monitor.recordPhaseTime(PerformanceMonitor.Phase.BLOCK_RENDER, System.nanoTime());
    }
}
