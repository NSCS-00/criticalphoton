package com.dlzstudio.criticalphoton.world;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.concurrent.locks.StampedLock;

/**
 * 区块缓存优化器
 * 使用 LRU 策略和扁平化数据结构优化区块访问
 */
public class ChunkCacheOptimizer {
    
    // 使用 fastutil 的高性能 HashMap
    private final Long2ReferenceLinkedOpenHashMap<LevelChunk> chunkCache;
    private final StampedLock lock;
    private final int maxCacheSize;
    
    // 访问热点统计
    private final long[] accessHotspots;
    private static final int HOTSPOT_REGION_SIZE = 32;
    
    public ChunkCacheOptimizer(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        this.chunkCache = new Long2ReferenceLinkedOpenHashMap<>(maxCacheSize, 0.75f);
        this.lock = new StampedLock();
        this.accessHotspots = new long[HOTSPOT_REGION_SIZE * HOTSPOT_REGION_SIZE];
    }
    
    /**
     * 获取区块（无锁快速路径）
     */
    public LevelChunk getChunkFast(long chunkPos) {
        long stamp = lock.tryOptimisticRead();
        LevelChunk chunk = chunkCache.get(chunkPos);
        if (lock.validate(stamp)) {
            return chunk;
        }
        // 降级为读锁
        stamp = lock.readLock();
        try {
            return chunkCache.get(chunkPos);
        } finally {
            lock.unlockRead(stamp);
        }
    }
    
    /**
     * 添加区块到缓存
     */
    public void putChunk(long chunkPos, LevelChunk chunk) {
        long stamp = lock.writeLock();
        try {
            if (chunkCache.size() >= maxCacheSize) {
                // LRU 淘汰：移除最久未使用的条目
                chunkCache.removeFirst();
            }
            chunkCache.put(chunkPos, chunk);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    /**
     * 移除区块
     */
    public void removeChunk(long chunkPos) {
        long stamp = lock.writeLock();
        try {
            chunkCache.remove(chunkPos);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    /**
     * 记录区块访问（用于热点分析）
     */
    public void recordAccess(int chunkX, int chunkZ) {
        int hotspotX = (chunkX & 0x1F);
        int hotspotZ = (chunkZ & 0x1F);
        int index = hotspotX + (hotspotZ << 5);
        accessHotspots[index]++;
    }
    
    /**
     * 获取热点区域访问计数
     */
    public long getHotspotAccess(int x, int z) {
        int index = (x & 0x1F) + ((z & 0x1F) << 5);
        return accessHotspots[index];
    }
    
    /**
     * 清除热点统计
     */
    public void clearHotspots() {
        for (int i = 0; i < accessHotspots.length; i++) {
            accessHotspots[i] = 0;
        }
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return chunkCache.size();
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        long stamp = lock.writeLock();
        try {
            chunkCache.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
