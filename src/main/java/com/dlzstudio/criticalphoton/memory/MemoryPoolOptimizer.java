package com.dlzstudio.criticalphoton.memory;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;

/**
 * 内存池优化器
 * 重用对象和缓冲区减少 GC 压力
 */
public class MemoryPoolOptimizer {
    
    // 直接内存池
    private final ByteBuffer[] directMemoryPool;
    private final boolean[] poolInUse;
    private final int poolSize;
    private final int bufferSize;
    
    // 对象池（泛型）
    private final Object[] objectPool;
    private final boolean[] objectInUse;
    private final int objectPoolSize;
    
    // 统计信息
    private int allocations;
    private int deallocations;
    private int poolHits;
    private int poolMisses;
    
    private static final Cleaner cleaner = Cleaner.create();
    
    public MemoryPoolOptimizer(int poolSize, int bufferSize, int objectPoolSize) {
        this.poolSize = poolSize;
        this.bufferSize = bufferSize;
        this.objectPoolSize = objectPoolSize;
        
        this.directMemoryPool = new ByteBuffer[poolSize];
        this.poolInUse = new boolean[poolSize];
        
        for (int i = 0; i < poolSize; i++) {
            directMemoryPool[i] = ByteBuffer.allocateDirect(bufferSize);
        }
        
        this.objectPool = new Object[objectPoolSize];
        this.objectInUse = new boolean[objectPoolSize];
        
        this.allocations = 0;
        this.deallocations = 0;
        this.poolHits = 0;
        this.poolMisses = 0;
    }
    
    /**
     * 获取直接内存缓冲区
     */
    public ByteBuffer acquireDirectBuffer() {
        allocations++;
        
        synchronized (this) {
            for (int i = 0; i < poolSize; i++) {
                if (!poolInUse[i]) {
                    poolInUse[i] = true;
                    poolHits++;
                    directMemoryPool[i].clear();
                    return directMemoryPool[i];
                }
            }
        }
        
        // 池耗尽，分配新缓冲区
        poolMisses++;
        return ByteBuffer.allocateDirect(bufferSize);
    }
    
    /**
     * 归还直接内存缓冲区
     */
    public void releaseDirectBuffer(ByteBuffer buffer) {
        synchronized (this) {
            for (int i = 0; i < poolSize; i++) {
                if (directMemoryPool[i] == buffer) {
                    poolInUse[i] = false;
                    deallocations++;
                    return;
                }
            }
        }
        // 非池缓冲区，依赖 GC 清理
    }
    
    /**
     * 获取对象（从池中）
     */
    @SuppressWarnings("unchecked")
    public <T> T acquireObject(T prototype) {
        allocations++;
        
        synchronized (this) {
            for (int i = 0; i < objectPoolSize; i++) {
                if (!objectInUse[i] && objectPool[i] != null) {
                    objectInUse[i] = true;
                    poolHits++;
                    return (T) objectPool[i];
                }
            }
        }
        
        poolMisses++;
        return prototype;
    }
    
    /**
     * 归还对象到池中
     */
    public void releaseObject(Object obj) {
        synchronized (this) {
            for (int i = 0; i < objectPoolSize; i++) {
                if (!objectInUse[i]) {
                    objectPool[i] = obj;
                    objectInUse[i] = false;
                    deallocations++;
                    return;
                }
            }
        }
    }
    
    /**
     * 注册清理器（用于自动清理）
     */
    public void registerCleaner(ByteBuffer buffer, Runnable cleanupAction) {
        cleaner.register(buffer, cleanupAction);
    }
    
    /**
     * 获取池命中率
     */
    public double getPoolHitRate() {
        int total = poolHits + poolMisses;
        return total > 0 ? (double) poolHits / total : 0;
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
            "Allocations: %d, Deallocations: %d, Pool Hits: %d, Pool Misses: %d, Hit Rate: %.2f%%",
            allocations, deallocations, poolHits, poolMisses, getPoolHitRate() * 100
        );
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        allocations = 0;
        deallocations = 0;
        poolHits = 0;
        poolMisses = 0;
    }
    
    /**
     * 获取池使用率
     */
    public double getPoolUsage() {
        int inUse = 0;
        for (boolean b : poolInUse) if (b) inUse++;
        return (double) inUse / poolSize;
    }
}
