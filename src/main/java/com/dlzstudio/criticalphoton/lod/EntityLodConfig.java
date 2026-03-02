package com.dlzstudio.criticalphoton.lod;

public class EntityLodConfig {
    public final float[] lodDistances;
    public final boolean enableMorphTransition;
    public final boolean enableAlphaTransition;
    
    public EntityLodConfig(float[] lodDistances, boolean enableMorph, boolean enableAlpha) {
        this.lodDistances = lodDistances;
        this.enableMorphTransition = enableMorph;
        this.enableAlphaTransition = enableAlpha;
    }
    
    public EntityLodConfig() {
        this(new float[]{32, 64, 128, 256, 512}, true, true);
    }
}
