package com.dlzstudio.criticalphoton.entity;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 实体组件系统（ECS 风格）
 * 使用数据导向设计优化实体更新
 */
public class EntityComponentSystem {
    
    // 组件数据存储（使用扁平数组）
    private final float[] positions;      // x, y, z
    private final float[] velocities;     // vx, vy, vz
    private final int[] entityStates;     // 状态标志
    private final int maxEntities;
    private final AtomicLongArray updateTimestamps;
    
    private static final int POSITION_STRIDE = 3;
    private static final int VELOCITY_STRIDE = 3;
    private static final int STATE_ACTIVE = 0x01;
    private static final int STATE_DIRTY = 0x02;
    
    public EntityComponentSystem(int maxEntities) {
        this.maxEntities = maxEntities;
        this.positions = new float[maxEntities * POSITION_STRIDE];
        this.velocities = new float[maxEntities * VELOCITY_STRIDE];
        this.entityStates = new int[maxEntities];
        this.updateTimestamps = new AtomicLongArray(maxEntities);
    }
    
    /**
     * 设置实体位置
     */
    public void setPosition(int entityId, float x, float y, float z) {
        int index = entityId * POSITION_STRIDE;
        positions[index] = x;
        positions[index + 1] = y;
        positions[index + 2] = z;
        entityStates[entityId] |= STATE_DIRTY;
    }
    
    /**
     * 获取实体位置
     */
    public float[] getPosition(int entityId) {
        int index = entityId * POSITION_STRIDE;
        return new float[] {
            positions[index],
            positions[index + 1],
            positions[index + 2]
        };
    }
    
    /**
     * 设置实体速度
     */
    public void setVelocity(int entityId, float vx, float vy, float vz) {
        int index = entityId * VELOCITY_STRIDE;
        velocities[index] = vx;
        velocities[index + 1] = vy;
        velocities[index + 2] = vz;
    }
    
    /**
     * 获取实体速度
     */
    public float[] getVelocity(int entityId) {
        int index = entityId * VELOCITY_STRIDE;
        return new float[] {
            velocities[index],
            velocities[index + 1],
            velocities[index + 2]
        };
    }
    
    /**
     * 激活实体
     */
    public void activateEntity(int entityId) {
        entityStates[entityId] |= STATE_ACTIVE;
        updateTimestamps.set(entityId, System.nanoTime());
    }
    
    /**
     * 停用实体
     */
    public void deactivateEntity(int entityId) {
        entityStates[entityId] &= ~STATE_ACTIVE;
    }
    
    /**
     * 检查实体是否激活
     */
    public boolean isActive(int entityId) {
        return (entityStates[entityId] & STATE_ACTIVE) != 0;
    }
    
    /**
     * 批量更新位置（SIMD 友好）
     */
    public void batchUpdatePositions(float dt) {
        for (int i = 0; i < maxEntities; i++) {
            if (isActive(i)) {
                int posIndex = i * POSITION_STRIDE;
                int velIndex = i * VELOCITY_STRIDE;
                
                // 位置 = 位置 + 速度 * dt
                positions[posIndex] += velocities[velIndex] * dt;
                positions[posIndex + 1] += velocities[velIndex + 1] * dt;
                positions[posIndex + 2] += velocities[velIndex + 2] * dt;
            }
        }
    }
    
    /**
     * 获取需要更新的实体（脏标记）
     */
    public int[] getDirtyEntities() {
        int count = 0;
        for (int i = 0; i < maxEntities; i++) {
            if ((entityStates[i] & STATE_DIRTY) != 0) {
                count++;
            }
        }
        
        int[] dirtyIds = new int[count];
        int index = 0;
        for (int i = 0; i < maxEntities; i++) {
            if ((entityStates[i] & STATE_DIRTY) != 0) {
                dirtyIds[index++] = i;
                entityStates[i] &= ~STATE_DIRTY; // 清除脏标记
            }
        }
        
        return dirtyIds;
    }
    
    /**
     * 获取最后更新时间
     */
    public long getLastUpdateTime(int entityId) {
        return updateTimestamps.get(entityId);
    }
    
    /**
     * 获取最大实体数
     */
    public int getMaxEntities() {
        return maxEntities;
    }
}
