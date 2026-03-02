package com.dlzstudio.criticalphoton.particle;

import net.minecraft.client.particle.Particle;

import java.util.Arrays;

/**
 * 粒子系统优化器
 * 使用对象池和扁平数组优化粒子管理
 */
public class ParticleOptimizer {
    
    // 粒子数据存储（扁平数组，避免对象开销）
    private final float[] particleX;
    private final float[] particleY;
    private final float[] particleZ;
    private final float[] particleVX;
    private final float[] particleVY;
    private final float[] particleVZ;
    private final float[] particleLife;
    private final float[] particleMaxLife;
    private final int[] particleState;
    private final Particle[] particleObjects;
    
    private final int maxParticles;
    private final boolean[] freeList;
    private int activeCount;
    
    // 状态标志
    private static final int STATE_ACTIVE = 0x01;
    private static final int STATE_NEEDS_UPDATE = 0x02;
    
    public ParticleOptimizer(int maxParticles) {
        this.maxParticles = maxParticles;
        this.particleX = new float[maxParticles];
        this.particleY = new float[maxParticles];
        this.particleZ = new float[maxParticles];
        this.particleVX = new float[maxParticles];
        this.particleVY = new float[maxParticles];
        this.particleVZ = new float[maxParticles];
        this.particleLife = new float[maxParticles];
        this.particleMaxLife = new float[maxParticles];
        this.particleState = new int[maxParticles];
        this.particleObjects = new Particle[maxParticles];
        this.freeList = new boolean[maxParticles];
        Arrays.fill(freeList, true);
        this.activeCount = 0;
    }
    
    /**
     * 分配粒子槽位（O(1)）
     */
    public int allocateParticle() {
        // 线性查找空闲槽位（简单且缓存友好）
        for (int i = 0; i < maxParticles; i++) {
            if (freeList[i]) {
                freeList[i] = false;
                activeCount++;
                return i;
            }
        }
        return -1; // 无空闲槽位
    }
    
    /**
     * 释放粒子槽位
     */
    public void freeParticle(int index) {
        if (index >= 0 && index < maxParticles && !freeList[index]) {
            freeList[index] = true;
            particleState[index] = 0;
            particleObjects[index] = null;
            activeCount--;
        }
    }
    
    /**
     * 设置粒子位置
     */
    public void setParticlePosition(int index, float x, float y, float z) {
        particleX[index] = x;
        particleY[index] = y;
        particleZ[index] = z;
    }
    
    /**
     * 设置粒子速度
     */
    public void setParticleVelocity(int index, float vx, float vy, float vz) {
        particleVX[index] = vx;
        particleVY[index] = vy;
        particleVZ[index] = vz;
    }
    
    /**
     * 设置粒子生命周期
     */
    public void setParticleLife(int index, float life, float maxLife) {
        particleLife[index] = life;
        this.particleMaxLife[index] = maxLife;
    }
    
    /**
     * 设置粒子对象
     */
    public void setParticleObject(int index, Particle particle) {
        particleObjects[index] = particle;
    }
    
    /**
     * 批量更新粒子（SIMD 友好）
     */
    public void batchUpdate(float dt) {
        for (int i = 0; i < maxParticles; i++) {
            if (!freeList[i] && (particleState[i] & STATE_ACTIVE) != 0) {
                // 更新位置
                particleX[i] += particleVX[i] * dt;
                particleY[i] += particleVY[i] * dt;
                particleZ[i] += particleVZ[i] * dt;
                
                // 更新生命周期
                particleLife[i] -= dt;
                
                // 检查是否死亡
                if (particleLife[i] <= 0) {
                    particleState[i] &= ~STATE_ACTIVE;
                    freeList[i] = true;
                    activeCount--;
                } else {
                    particleState[i] |= STATE_NEEDS_UPDATE;
                }
            }
        }
    }
    
    /**
     * 获取活跃粒子数量
     */
    public int getActiveCount() {
        return activeCount;
    }
    
    /**
     * 获取最大粒子数
     */
    public int getMaxParticles() {
        return maxParticles;
    }
    
    /**
     * 获取粒子位置数组（直接访问，零拷贝）
     */
    public float[] getParticleX() {
        return particleX;
    }
    
    public float[] getParticleY() {
        return particleY;
    }
    
    public float[] getParticleZ() {
        return particleZ;
    }
    
    /**
     * 获取粒子对象
     */
    public Particle getParticleObject(int index) {
        return particleObjects[index];
    }
    
    /**
     * 清除所有粒子
     */
    public void clear() {
        Arrays.fill(freeList, true);
        Arrays.fill(particleState, 0);
        Arrays.fill(particleObjects, null);
        activeCount = 0;
    }
}
