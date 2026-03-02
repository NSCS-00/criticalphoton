package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.particle.ParticleOptimizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 粒子系统优化 Mixin
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleOptimizerMixin {
    
    private static ParticleOptimizer particleOptimizer;
    private static boolean initialized;
    
    private void initOptimizer() {
        if (!initialized) {
            particleOptimizer = new ParticleOptimizer(16384); // 最大 16K 粒子
            initialized = true;
        }
    }
    
    /**
     * 优化粒子添加
     */
    @Inject(method = "add", at = @At("HEAD"))
    public void addParticleOptimized(Particle particle, CallbackInfo ci) {
        initOptimizer();
        
        int slot = particleOptimizer.allocateParticle();
        if (slot >= 0) {
            particleOptimizer.setParticlePosition(slot, 
                (float) particle.getX(), 
                (float) particle.getY(), 
                (float) particle.getZ());
            particleOptimizer.setParticleObject(slot, particle);
        }
    }
    
    /**
     * 优化粒子 Tick
     */
    @Inject(method = "tick", at = @At("HEAD"))
    public void tickParticlesOptimized(CallbackInfo ci) {
        if (particleOptimizer != null) {
            float dt = Minecraft.getInstance().getFrameTime() / 1000.0f;
            particleOptimizer.batchUpdate(dt);
        }
    }
    
    /**
     * 优化粒子清除
     */
    @Inject(method = "clearParticles", at = @At("TAIL"))
    public void clearParticlesOptimized(CallbackInfo ci) {
        if (particleOptimizer != null) {
            particleOptimizer.clear();
        }
    }
}
