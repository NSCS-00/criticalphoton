package com.dlzstudio.criticalphoton.lod;

import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连续 LOD 管理器
 */
public class LodManager {
    
    public enum LodLevel {
        LOD0(0, 1.0f), LOD1(1, 0.8f), LOD2(2, 0.6f),
        LOD3(3, 0.4f), LOD4(4, 0.2f), LOD5(5, 0.1f);
        
        public final int index;
        public final float detailFactor;
        
        LodLevel(int index, float detailFactor) {
            this.index = index;
            this.detailFactor = detailFactor;
        }
        
        public static LodLevel fromIndex(int index) {
            if (index < 0) return LOD0;
            if (index >= values().length) return LOD5;
            return values()[index];
        }
    }
    
    private final Map<EntityType<?>, EntityLodConfig> entityConfigs;
    private final Map<Entity, EntityLodState> entityLodStates;
    private final float screenSpaceErrorThreshold;
    private final int minSwitchFrames;
    
    public LodManager() {
        this.entityConfigs = new ConcurrentHashMap<>();
        this.entityLodStates = new ConcurrentHashMap<>();
        this.screenSpaceErrorThreshold = CriticalPhotonConfig.CLIENT.lodScreenSpaceError.get().floatValue();
        this.minSwitchFrames = CriticalPhotonConfig.CLIENT.lodMinSwitchFrames.get();
        registerDefaultConfigs();
    }
    
    private void registerDefaultConfigs() {
        registerConfig(EntityType.ZOMBIE, new EntityLodConfig(new float[]{16, 32, 64, 128, 256}, true, true));
        registerConfig(EntityType.SKELETON, new EntityLodConfig(new float[]{16, 32, 64, 128, 256}, true, true));
        registerConfig(EntityType.CREEPER, new EntityLodConfig(new float[]{16, 32, 64, 128, 256}, true, true));
        registerConfig(EntityType.ENDERMAN, new EntityLodConfig(new float[]{20, 40, 80, 160, 320}, true, true));
        registerConfig(EntityType.COW, new EntityLodConfig(new float[]{12, 24, 48, 96, 192}, true, true));
        registerConfig(EntityType.PIG, new EntityLodConfig(new float[]{12, 24, 48, 96, 192}, true, true));
    }
    
    public void registerConfig(EntityType<?> entityType, EntityLodConfig config) {
        entityConfigs.put(entityType, config);
    }
    
    public EntityLodState getLodState(Entity entity) {
        return entityLodStates.computeIfAbsent(entity, e -> new EntityLodState(minSwitchFrames));
    }
    
    public void update(Entity entity, double cameraDistance, float partialTick) {
        EntityLodState state = getLodState(entity);
        EntityLodConfig config = getOrCreateConfig(entity.getType());
        float screenError = calculateScreenSpaceError(entity, cameraDistance);
        int targetLod = calculateTargetLod(config, screenError, cameraDistance);
        state.update(targetLod, partialTick);
    }
    
    private float calculateScreenSpaceError(Entity entity, double distance) {
        float entitySize = Math.max(entity.getBbWidth(), entity.getBbHeight());
        float fov = net.minecraft.client.Minecraft.getInstance().options.fov().get().floatValue();
        return (entitySize * 100.0f) / (float) (distance * fov / 90.0f);
    }
    
    private int calculateTargetLod(EntityLodConfig config, float screenError, double distance) {
        if (screenError <= screenSpaceErrorThreshold) return 0;
        float[] distances = config.lodDistances;
        for (int i = 0; i < distances.length; i++) {
            if (distance < distances[i]) return i;
        }
        return LodLevel.LOD5.index;
    }
    
    public EntityLodConfig getOrCreateConfig(EntityType<?> entityType) {
        return entityConfigs.computeIfAbsent(entityType, type -> 
            new EntityLodConfig(new float[]{32, 64, 128, 256, 512}, true, true));
    }
    
    public int getCurrentLodLevel(Entity entity) {
        EntityLodState state = entityLodStates.get(entity);
        return state != null ? state.getCurrentLod() : 0;
    }
    
    public float getInterpolatedLodLevel(Entity entity) {
        EntityLodState state = entityLodStates.get(entity);
        return state != null ? state.getInterpolatedLod() : 0.0f;
    }
    
    public float getLodTransitionProgress(Entity entity) {
        EntityLodState state = entityLodStates.get(entity);
        return state != null ? state.getTransitionProgress() : 1.0f;
    }
    
    public void cleanup() {
        entityLodStates.keySet().removeIf(Entity::isRemoved);
    }
    
    public void reset() {
        entityLodStates.clear();
    }
}
