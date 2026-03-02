package com.dlzstudio.criticalphoton.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 方块更新优化器
 * 使用批量处理和延迟更新优化方块更新
 */
public class BlockUpdateOptimizer {
    
    // 更新队列（环形缓冲区）
    private final long[] updateQueue;
    private final BlockState[] stateQueue;
    private final int capacity;
    private int head;
    private int tail;
    
    // 批量更新缓冲区
    private final BlockPos[] batchBuffer;
    private final BlockState[] batchStates;
    private int batchSize;
    private static final int MAX_BATCH_SIZE = 256;
    
    // 统计
    private int updatesProcessed;
    private int batchesProcessed;
    private int redundantUpdates;
    
    // 去重位图
    private final long[] dedupBitmap;
    private static final int BITMAP_SIZE = 4096;
    
    public BlockUpdateOptimizer(int capacity) {
        this.capacity = capacity;
        this.updateQueue = new long[capacity];
        this.stateQueue = new BlockState[capacity];
        this.batchBuffer = new BlockPos[MAX_BATCH_SIZE];
        this.batchStates = new BlockState[MAX_BATCH_SIZE];
        this.batchSize = 0;
        this.dedupBitmap = new long[BITMAP_SIZE];
    }
    
    /**
     * 添加方块更新
     */
    public void addUpdate(BlockPos pos, BlockState state) {
        long packed = packPosition(pos);
        int hash = hashPosition(pos) & (BITMAP_SIZE * 64 - 1);
        
        // 检查是否重复
        if (isDuplicate(hash)) {
            redundantUpdates++;
            return;
        }
        
        setDuplicate(hash);
        
        int nextTail = (tail + 1) % capacity;
        if (nextTail != head) {
            updateQueue[tail] = packed;
            stateQueue[tail] = state;
            tail = nextTail;
        }
        
        // 添加到批量缓冲区
        if (batchSize < MAX_BATCH_SIZE) {
            batchBuffer[batchSize] = pos.immutable();
            batchStates[batchSize] = state;
            batchSize++;
        }
    }
    
    /**
     * 处理批量更新
     */
    public BlockPos[] processBatch() {
        if (batchSize == 0) return BlockPos.ZERO::iterator;
        
        BlockPos[] result = Arrays.copyOf(batchBuffer, batchSize);
        batchSize = 0;
        batchesProcessed++;
        
        return result;
    }
    
    /**
     * 打包位置
     */
    private long packPosition(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
    }
    
    /**
     * 哈希位置
     */
    private int hashPosition(BlockPos pos) {
        int hash = pos.getX() * 31337 + pos.getY() * 8192 + pos.getZ() * 65537;
        hash ^= (hash >>> 16);
        return hash & 0x7FFFFFFF;
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(int hash) {
        int wordIndex = hash / 64;
        int bitIndex = hash % 64;
        return (dedupBitmap[wordIndex] & (1L << bitIndex)) != 0;
    }
    
    /**
     * 标记为重复
     */
    private void setDuplicate(int hash) {
        int wordIndex = hash / 64;
        int bitIndex = hash % 64;
        dedupBitmap[wordIndex] |= (1L << bitIndex);
    }
    
    /**
     * 清除去重位图
     */
    public void clearDedup() {
        Arrays.fill(dedupBitmap, 0);
    }
    
    /**
     * 获取队列中更新数量
     */
    public int getQueueSize() {
        if (tail >= head) {
            return tail - head;
        } else {
            return capacity - head + tail;
        }
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
            "Processed: %d, Batches: %d, Redundant: %d (%.1f%% saved)",
            updatesProcessed, batchesProcessed, redundantUpdates,
            capacity > 0 ? (float) redundantUpdates / (updatesProcessed + redundantUpdates) * 100 : 0
        );
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        updatesProcessed = 0;
        batchesProcessed = 0;
        redundantUpdates = 0;
    }
}
