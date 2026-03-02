package com.dlzstudio.criticalphoton.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 方块状态缓存优化器
 * 使用扁平数组和位压缩优化方块存储
 */
public class BlockStateCacheOptimizer {
    
    // 使用一维数组存储方块状态（避免多维数组开销）
    private final BlockState[] blockStates;
    private final int size;
    private final int bitMask;
    
    // 空方块状态缓存（避免 null 检查）
    private static final BlockState EMPTY_STATE = null;
    
    public BlockStateCacheOptimizer(int size) {
        // 大小必须是 2 的幂
        this.size = Integer.highestOneBit(size - 1) << 1;
        this.bitMask = this.size - 1;
        this.blockStates = new BlockState[this.size];
        Arrays.fill(this.blockStates, EMPTY_STATE);
    }
    
    /**
     * 快速获取方块状态（无边界检查）
     */
    public BlockState getFast(int x, int y, int z) {
        int index = hashPosition(x, y, z);
        return blockStates[index];
    }
    
    /**
     * 设置方块状态
     */
    public void set(int x, int y, int z, BlockState state) {
        int index = hashPosition(x, y, z);
        blockStates[index] = state;
    }
    
    /**
     * 哈希位置到索引（使用混合函数减少冲突）
     */
    private int hashPosition(int x, int y, int z) {
        int hash = x * 31337 + y * 8192 + z * 65537;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        return hash & bitMask;
    }
    
    /**
     * 批量获取方块状态（SIMD 友好）
     */
    public void getBatch(int[] positions, BlockState[] results) {
        for (int i = 0; i < positions.length; i += 4) {
            int pos = positions[i];
            int x = pos & 0xFF;
            int y = (pos >>> 8) & 0xFF;
            int z = (pos >>> 16) & 0xFF;
            results[i] = getFast(x, y, z);
            
            if (i + 1 < positions.length) {
                pos = positions[i + 1];
                x = pos & 0xFF;
                y = (pos >>> 8) & 0xFF;
                z = (pos >>> 16) & 0xFF;
                results[i + 1] = getFast(x, y, z);
            }
            if (i + 2 < positions.length) {
                pos = positions[i + 2];
                x = pos & 0xFF;
                y = (pos >>> 8) & 0xFF;
                z = (pos >>> 16) & 0xFF;
                results[i + 2] = getFast(x, y, z);
            }
            if (i + 3 < positions.length) {
                pos = positions[i + 3];
                x = pos & 0xFF;
                y = (pos >>> 8) & 0xFF;
                z = (pos >>> 16) & 0xFF;
                results[i + 3] = getFast(x, y, z);
            }
        }
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        Arrays.fill(blockStates, EMPTY_STATE);
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return size;
    }
}
