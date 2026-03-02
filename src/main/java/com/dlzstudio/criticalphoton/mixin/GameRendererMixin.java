package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    public void preRender(float partialTick, long nanoTime, boolean tick, CallbackInfo ci) {
        PerformanceMonitor monitor = CriticalPhoton.getInstance().getPerformanceMonitor();
        monitor.recordPhaseTime(PerformanceMonitor.Phase.RENDER_SETUP, System.nanoTime());
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    public void postRender(float partialTick, long nanoTime, boolean tick, CallbackInfo ci) {
        PerformanceMonitor monitor = CriticalPhoton.getInstance().getPerformanceMonitor();
        monitor.recordPhaseTime(PerformanceMonitor.Phase.TOTAL, System.nanoTime() - nanoTime);
    }
}
