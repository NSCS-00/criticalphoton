package com.dlzstudio.criticalphoton.system;

import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import net.minecraft.util.Mth;

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
        this.lodTransitionProgress = 1.0f;
    }
    
    public float calculateAlpha(boolean isVisible, float partialTick) {
        if (!config.isAlphaTransitionEnabled()) return isVisible ? 1.0f : config.getMinAlpha();
        
        long currentTime = System.currentTimeMillis();
        if (isVisible != wasVisible) {
            wasVisible = isVisible;
            transitionStartTime = currentTime;
            targetAlpha = isVisible ? 1.0f : config.getMinAlpha();
        }
        
        float transitionTime = isVisible ? config.getFadeInTime() : config.getFadeOutTime();
        float progress = Math.min(1.0f, (float)(currentTime - transitionStartTime) / (transitionTime * 1000));
        float eased = progress < 0.5f ? 4 * progress * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 3) / 2;
        currentAlpha = Mth.lerp(eased, wasVisible ? config.getMinAlpha() : 1.0f, targetAlpha);
        return currentAlpha;
    }
    
    public int getLodLevel(float distance) {
        int targetLodLevel = config.getLodLevel(distance);
        if (targetLodLevel != currentLodLevel && config.isMorphTransitionEnabled()) {
            previousLodLevel = currentLodLevel;
            currentLodLevel = targetLodLevel;
            lodTransitionProgress = 0.0f;
        }
        if (lodTransitionProgress < 1.0f) lodTransitionProgress = Math.min(1.0f, lodTransitionProgress + 0.1f);
        return currentLodLevel;
    }
    
    public float getLodTransitionProgress() { return lodTransitionProgress; }
    public float getInterpolatedLodLevel() { return Mth.lerp(lodTransitionProgress, previousLodLevel, currentLodLevel); }
}
