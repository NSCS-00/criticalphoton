package com.dlzstudio.criticalphoton.system;

import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import net.minecraft.util.Mth;

/**
 * 实体可见性状态
 */
public class EntityVisibilityState {
    
    private final VisualAnchorConfig config;
    private float currentAlpha, targetAlpha;
    private long transitionStartTime;
    private boolean wasVisible;
    private int currentLodLevel, previousLodLevel;
    private float lodTransitionProgress;
    
    public EntityVisibilityState(VisualAnchorConfig config) {
        this.config = config;
        this.currentAlpha = 1.0f;
        this.targetAlpha = 1.0f;
        this.transitionStartTime = 0;
        this.wasVisible = true;
        this.currentLodLevel = 0;
        this.previousLodLevel = 0;
        this.lodTransitionProgress = 1.0f;
    }
    
    public float calculateAlpha(boolean isVisible, float partialTick) {
        if (!config.isAlphaTransitionEnabled()) {
            return isVisible ? 1.0f : config.getMinAlpha();
        }
        
        long currentTime = System.currentTimeMillis();
        if (isVisible != wasVisible) {
            wasVisible = isVisible;
            transitionStartTime = currentTime;
            targetAlpha = isVisible ? 1.0f : config.getMinAlpha();
        }
        
        float transitionTime = isVisible ? config.getFadeInTime() : config.getFadeOutTime();
        long elapsed = currentTime - transitionStartTime;
        float progress = Math.min(1.0f, (float) elapsed / (transitionTime * 1000));
        float easedProgress = easeInOutCubic(progress);
        currentAlpha = Mth.lerp(easedProgress, wasVisible ? config.getMinAlpha() : 1.0f, targetAlpha);
        return currentAlpha;
    }
    
    public int getLodLevel(float distance) {
        int targetLodLevel = config.getLodLevel(distance);
        if (targetLodLevel != currentLodLevel && config.isMorphTransitionEnabled()) {
            previousLodLevel = currentLodLevel;
            currentLodLevel = targetLodLevel;
            lodTransitionProgress = 0.0f;
        }
        if (lodTransitionProgress < 1.0f) {
            lodTransitionProgress += 0.1f;
            lodTransitionProgress = Math.min(1.0f, lodTransitionProgress);
        }
        return currentLodLevel;
    }
    
    public float getLodTransitionProgress() { return lodTransitionProgress; }
    public float getInterpolatedLodLevel() { return Mth.lerp(lodTransitionProgress, previousLodLevel, currentLodLevel); }
    
    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
    
    public void reset() {
        currentAlpha = 1.0f; targetAlpha = 1.0f; wasVisible = true;
        currentLodLevel = 0; previousLodLevel = 0; lodTransitionProgress = 1.0f;
    }
}
