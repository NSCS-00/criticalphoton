package com.dlzstudio.criticalphoton.lod;

import net.minecraft.util.Mth;

public class EntityLodState {
    private int currentLod;
    private int previousLod;
    private float transitionProgress;
    private int targetLod;
    private int framesAtCurrentLod;
    private final int minSwitchFrames;
    private static final float TRANSITION_SPEED = 0.1f;
    
    public EntityLodState(int minSwitchFrames) {
        this.currentLod = 0;
        this.previousLod = 0;
        this.transitionProgress = 1.0f;
        this.targetLod = 0;
        this.framesAtCurrentLod = 0;
        this.minSwitchFrames = minSwitchFrames;
    }
    
    public void update(int newTargetLod, float partialTick) {
        if (newTargetLod != targetLod) {
            targetLod = newTargetLod;
            if (transitionProgress >= 1.0f && targetLod != currentLod) {
                previousLod = currentLod;
                transitionProgress = 0.0f;
                framesAtCurrentLod = 0;
            }
        }
        
        if (transitionProgress < 1.0f) {
            transitionProgress += TRANSITION_SPEED;
            transitionProgress = Math.min(1.0f, transitionProgress);
        }
        
        if (transitionProgress >= 1.0f) {
            currentLod = targetLod;
            framesAtCurrentLod++;
        }
    }
    
    public int getCurrentLod() { return currentLod; }
    public float getInterpolatedLod() { return Mth.lerp(transitionProgress, previousLod, currentLod); }
    public float getTransitionProgress() { return transitionProgress; }
    public boolean canSwitchLod() { return framesAtCurrentLod >= minSwitchFrames && transitionProgress >= 1.0f; }
    public void reset() {
        currentLod = 0; previousLod = 0; transitionProgress = 1.0f; targetLod = 0; framesAtCurrentLod = 0;
    }
}
