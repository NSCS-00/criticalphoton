package com.dlzstudio.criticalphoton.world;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightChunk;

import java.util.Arrays;

/**
 * 光照数据优化器
 * 使用位压缩和延迟更新优化光照计算
 */
public class LightDataOptimizer {
    
    // 压缩的光照数据（每块 4 位）
    private final byte[] blockLight;
    private final byte[] skyLight;
    private final int size;
    
    // 脏标记（延迟更新）
    private final boolean[] dirtySections;
    private int dirtyCount;
    
    // 光照更新队列
    private final int[] updateQueue;
    private int queueHead;
    private int queueTail;
    private static final int QUEUE_CAPACITY = 4096;
    
    public LightDataOptimizer(int size) {
        this.size = size;
        // 每个方块 4 位 = 每字节存储 2 个方块
        this.blockLight = new byte[(size * size * size) / 2];
        this.skyLight = new byte[(size * size * size) / 2];
        this.dirtySections = new boolean[64];
        this.dirtyCount = 0;
        this.updateQueue = new int[QUEUE_CAPACITY];
        this.queueHead = 0;
        this.queueTail = 0;
    }
    
    /**
     * 获取方块光照（0-15）
     */
    public int getBlockLight(int x, int y, int z) {
        int index = ((y << 8) | (z << 4) | x) >> 1;
        byte value = blockLight[index];
        if ((x & 1) == 0) {
            return value & 0x0F;
        } else {
            return (value >> 4) & 0x0F;
        }
    }
    
    /**
     * 设置方块光照（0-15）
     */
    public void setBlockLight(int x, int y, int z, int level) {
        int index = ((y << 8) | (z << 4) | x) >> 1;
        byte value = blockLight[index];
        if ((x & 1) == 0) {
            value = (byte) ((value & 0xF0) | (level & 0x0F));
        } else {
            value = (byte) ((value & 0x0F) | ((level & 0x0F) << 4));
        }
        blockLight[index] = value;
        markSectionDirty(x >> 4, y >> 4, z >> 4);
    }
    
    /**
     * 获取天空光照
     */
    public int getSkyLight(int x, int y, int z) {
        int index = ((y << 8) | (z << 4) | x) >> 1;
        byte value = skyLight[index];
        return ((x & 1) == 0) ? (value & 0x0F) : ((value >> 4) & 0x0F);
    }
    
    /**
     * 设置天空光照
     */
    public void setSkyLight(int x, int y, int z, int level) {
        int index = ((y << 8) | (z << 4) | x) >> 1;
        byte value = skyLight[index];
        if ((x & 1) == 0) {
            value = (byte) ((value & 0xF0) | (level & 0x0F));
        } else {
            value = (byte) ((value & 0x0F) | ((level & 0x0F) << 4));
        }
        skyLight[index] = value;
    }
    
    /**
     * 标记区块为脏（需要更新）
     */
    private void markSectionDirty(int sectionX, int sectionY, int sectionZ) {
        int sectionIndex = SectionPos.asLong(sectionX, sectionY, sectionZ) & 0x3F;
        if (!dirtySections[sectionIndex]) {
            dirtySections[sectionIndex] = true;
            dirtyCount++;
        }
    }
    
    /**
     * 添加光照更新到队列
     */
    public void queueLightUpdate(int x, int y, int z, int level) {
        int packed = (x & 0xFF) | ((y & 0xFF) << 8) | ((z & 0xFF) << 16) | ((level & 0x0F) << 24);
        int nextTail = (queueTail + 1) & (QUEUE_CAPACITY - 1);
        if (nextTail != queueHead) {
            updateQueue[queueTail] = packed;
            queueTail = nextTail;
        }
    }
    
    /**
     * 处理光照更新队列
     */
    public void processLightQueue() {
        while (queueHead != queueTail) {
            int packed = updateQueue[queueHead];
            queueHead = (queueHead + 1) & (QUEUE_CAPACITY - 1);
            
            int x = packed & 0xFF;
            int y = (packed >>> 8) & 0xFF;
            int z = (packed >>> 16) & 0xFF;
            int level = (packed >>> 24) & 0x0F;
            
            setBlockLight(x, y, z, level);
        }
    }
    
    /**
     * 获取脏区块数量
     */
    public int getDirtyCount() {
        return dirtyCount;
    }
    
    /**
     * 清除脏标记
     */
    public void clearDirty() {
        Arrays.fill(dirtySections, false);
        dirtyCount = 0;
    }
}
