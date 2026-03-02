package com.dlzstudio.criticalphoton.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实体 ID 分配器优化
 * 使用位图和循环分配避免 ID 冲突和内存泄漏
 */
public class EntityIdAllocator {
    
    // 使用位图跟踪已使用的 ID
    private final long[] usedIds;
    private final int maxId;
    private final int wordCount;
    
    // 最后分配的 ID（用于循环查找）
    private final AtomicInteger lastAllocatedId;
    
    // ID 到实体的映射（使用 fastutil）
    private final Int2ObjectLinkedOpenHashMap<Entity> idToEntity;
    
    private static final int BITS_PER_WORD = 64;
    
    public EntityIdAllocator(int maxId) {
        this.maxId = maxId;
        this.wordCount = (maxId + BITS_PER_WORD - 1) / BITS_PER_WORD;
        this.usedIds = new long[wordCount];
        this.lastAllocatedId = new AtomicInteger(0);
        this.idToEntity = new Int2ObjectLinkedOpenHashMap<>(1024, 0.75f);
    }
    
    /**
     * 分配新 ID（无锁快速路径）
     */
    public int allocateId() {
        int startId = lastAllocatedId.get();
        
        // 从上次分配的位置开始查找
        for (int offset = 0; offset < maxId; offset++) {
            int id = (startId + offset) % maxId;
            if (trySetId(id)) {
                lastAllocatedId.set(id);
                return id;
            }
        }
        
        // 如果没有找到空闲 ID，返回 -1
        return -1;
    }
    
    /**
     * 尝试设置 ID（原子操作）
     */
    private boolean trySetId(int id) {
        int wordIndex = id / BITS_PER_WORD;
        int bitIndex = id % BITS_PER_WORD;
        long bitMask = 1L << bitIndex;
        
        synchronized (this) {
            if ((usedIds[wordIndex] & bitMask) == 0) {
                usedIds[wordIndex] |= bitMask;
                return true;
            }
        }
        return false;
    }
    
    /**
     * 释放 ID
     */
    public void freeId(int id) {
        int wordIndex = id / BITS_PER_WORD;
        int bitIndex = id % BITS_PER_WORD;
        long bitMask = ~(1L << bitIndex);
        
        synchronized (this) {
            usedIds[wordIndex] &= bitMask;
        }
        idToEntity.remove(id);
    }
    
    /**
     * 注册实体
     */
    public void registerEntity(int id, Entity entity) {
        idToEntity.put(id, entity);
    }
    
    /**
     * 获取实体
     */
    public Entity getEntity(int id) {
        return idToEntity.get(id);
    }
    
    /**
     * 移除实体
     */
    public Entity removeEntity(int id) {
        Entity entity = idToEntity.remove(id);
        if (entity != null) {
            freeId(id);
        }
        return entity;
    }
    
    /**
     * 检查 ID 是否已使用
     */
    public boolean isIdUsed(int id) {
        int wordIndex = id / BITS_PER_WORD;
        int bitIndex = id % BITS_PER_WORD;
        return (usedIds[wordIndex] & (1L << bitIndex)) != 0;
    }
    
    /**
     * 获取已分配 ID 数量
     */
    public int getAllocatedCount() {
        int count = 0;
        for (long word : usedIds) {
            count += Long.bitCount(word);
        }
        return count;
    }
    
    /**
     * 获取实体数量
     */
    public int getEntityCount() {
        return idToEntity.size();
    }
}
