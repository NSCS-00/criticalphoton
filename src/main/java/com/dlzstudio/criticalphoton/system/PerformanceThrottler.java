package com.dlzstudio.criticalphoton.system;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import org.slf4j.Logger;

public class PerformanceThrottler {
    private static final Logger LOGGER = CriticalPhoton.LOGGER;
    private final int targetFps, minFps;
    private int throttleLevel, lodBias, entityUpdateInterval;
    private float particleDensity;
    private long lastAdjustment;
    private final int[] fpsHistory;
    private int fpsIndex;
    private static final long ADJUSTMENT_COOLDOWN = 5000;
    
    public PerformanceThrottler() {
        this.targetFps = CriticalPhotonConfig.CLIENT.targetFps.get();
        this.minFps = CriticalPhotonConfig.CLIENT.minFps.get();
        this.fpsHistory = new int[10];
    }
    
    public void update(int currentFps, PerformanceMonitor.FrameMetrics metrics) {
        fpsHistory[fpsIndex++ % fpsHistory.length] = currentFps;
        if (System.currentTimeMillis() - lastAdjustment < ADJUSTMENT_COOLDOWN) return;
        
        int avgFps = getAverageFps();
        adjustThrottleLevel(avgFps);
    }
    
    private int getAverageFps() {
        int sum = 0, count = 0;
        for (int fps : fpsHistory) { if (fps > 0) { sum += fps; count++; } }
        return count > 0 ? sum / count : 0;
    }
    
    private void adjustThrottleLevel(int avgFps) {
        int newLevel = throttleLevel, newLodBias = lodBias, newUpdateInterval = entityUpdateInterval;
        float newParticleDensity = particleDensity;
        
        if (avgFps < minFps) {
            newLevel = Math.min(3, throttleLevel + 1);
            newLodBias = Math.min(2, lodBias + 1);
            if (avgFps < minFps / 2) { newUpdateInterval = 2; newParticleDensity = 0.5f; }
            LOGGER.info("性能不足，提升节流级别：{}", newLevel);
        } else if (avgFps > targetFps * 1.5 && throttleLevel > 0) {
            newLevel = Math.max(0, throttleLevel - 1);
            newLodBias = Math.max(0, lodBias - 1);
            newUpdateInterval = 1;
            newParticleDensity = 1.0f;
            LOGGER.info("性能充足，降低节流级别：{}", newLevel);
        }
        
        if (newLevel != throttleLevel) {
            throttleLevel = newLevel; lodBias = newLodBias;
            entityUpdateInterval = newUpdateInterval; particleDensity = newParticleDensity;
            lastAdjustment = System.currentTimeMillis();
        }
    }
    
    public int getThrottleLevel() { return throttleLevel; }
    public int getLodBias() { return lodBias; }
    public int getEntityUpdateInterval() { return entityUpdateInterval; }
    public float getParticleDensity() { return particleDensity; }
    public boolean shouldUpdateEntity(int entityIndex) {
        return entityUpdateInterval <= 1 || (entityIndex % entityUpdateInterval) == 0;
    }
    public void reset() {
        throttleLevel = 0; lodBias = 0; entityUpdateInterval = 1; particleDensity = 1.0f;
    }
}
