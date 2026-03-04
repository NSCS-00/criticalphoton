package com.dlzstudio.criticalphoton.graph;

public class VisualAnchorConfig {
    private final float fadeInTime;
    private final float fadeOutTime;
    private final float minAlpha;
    private final float maxRenderDistance;
    private final float[] lodDistances;
    private final boolean enableMorphTransition;
    private final boolean enableAlphaTransition;
    
    public VisualAnchorConfig(Builder builder) {
        this.fadeInTime = builder.fadeInTime;
        this.fadeOutTime = builder.fadeOutTime;
        this.minAlpha = builder.minAlpha;
        this.maxRenderDistance = builder.maxRenderDistance;
        this.lodDistances = builder.lodDistances;
        this.enableMorphTransition = builder.enableMorphTransition;
        this.enableAlphaTransition = builder.enableAlphaTransition;
    }
    
    public float getFadeInTime() { return fadeInTime; }
    public float getFadeOutTime() { return fadeOutTime; }
    public float getMinAlpha() { return minAlpha; }
    public float getMaxRenderDistance() { return maxRenderDistance; }
    public float[] getLodDistances() { return lodDistances; }
    public boolean isMorphTransitionEnabled() { return enableMorphTransition; }
    public boolean isAlphaTransitionEnabled() { return enableAlphaTransition; }
    
    public int getLodLevel(float distance) {
        if (lodDistances == null || lodDistances.length == 0) return 0;
        for (int i = 0; i < lodDistances.length; i++) {
            if (distance < lodDistances[i]) return i;
        }
        return Math.min(lodDistances.length, 5);
    }
    
    public static class Builder {
        private float fadeInTime = 0.3f;
        private float fadeOutTime = 0.5f;
        private float minAlpha = 0.1f;
        private float maxRenderDistance = 256.0f;
        private float[] lodDistances = {32.0f, 64.0f, 128.0f, 192.0f, 256.0f};
        private boolean enableMorphTransition = true;
        private boolean enableAlphaTransition = true;
        
        public Builder fadeInTime(float time) { this.fadeInTime = time; return this; }
        public Builder fadeOutTime(float time) { this.fadeOutTime = time; return this; }
        public Builder minAlpha(float alpha) { this.minAlpha = alpha; return this; }
        public Builder maxRenderDistance(float distance) { this.maxRenderDistance = distance; return this; }
        public Builder lodDistances(float... distances) { this.lodDistances = distances; return this; }
        public Builder enableMorphTransition(boolean enable) { this.enableMorphTransition = enable; return this; }
        public Builder enableAlphaTransition(boolean enable) { this.enableAlphaTransition = enable; return this; }
        public VisualAnchorConfig build() { return new VisualAnchorConfig(this); }
    }
}
