package com.dlzstudio.criticalphoton.block;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

/**
 * 方块实体（Tile Entity）优化器
 * 使用惰性更新和距离排序优化方块实体处理
 */
public class TileEntityOptimizer {
    
    // 方块实体位置存储（压缩为 short）
    private final ShortList activePositions;
    private final ShortList inactivePositions;
    
    // 更新间隔配置
    private final int[] updateIntervals;
    private final long[] lastUpdateTimes;
    
    // 距离玩家最近的方块实体优先
    private int playerChunkX;
    private int playerChunkZ;
    
    // 统计
    private int activeCount;
    private int inactiveCount;
    private int updatesSkipped;
    
    public TileEntityOptimizer(int initialCapacity) {
        this.activePositions = new ShortArrayList(initialCapacity);
        this.inactivePositions = new ShortArrayList(initialCapacity);
        this.updateIntervals = new int[65536];
        this.lastUpdateTimes = new long[65536];
        this.playerChunkX = 0;
        this.playerChunkZ = 0;
    }
    
    /**
     * 添加活跃方块实体
     */
    public void addActive(short packedPos, int updateInterval) {
        activePositions.add(packedPos);
        updateIntervals[packedPos & 0xFFFF] = updateInterval;
        lastUpdateTimes[packedPos & 0xFFFF] = System.currentTimeMillis();
        activeCount++;
    }
    
    /**
     * 移除方块实体
     */
    public void remove(short packedPos) {
        activePositions.removeIf(pos -> pos == packedPos);
        inactivePositions.removeIf(pos -> pos == packedPos);
        activeCount--;
    }
    
    /**
     * 检查是否需要更新
     */
    public boolean needsUpdate(short packedPos, long currentTime) {
        int interval = updateIntervals[packedPos & 0xFFFF];
        long lastUpdate = lastUpdateTimes[packedPos & 0xFFFF];
        return (currentTime - lastUpdate) >= interval;
    }
    
    /**
     * 标记为已更新
     */
    public void markUpdated(short packedPos) {
        lastUpdateTimes[packedPos & 0xFFFF] = System.currentTimeMillis();
    }
    
    /**
     * 设置玩家位置（用于优先级排序）
     */
    public void setPlayerChunk(int chunkX, int chunkZ) {
        this.playerChunkX = chunkX;
        this.playerChunkZ = chunkZ;
    }
    
    /**
     * 获取需要更新的方块实体（按优先级排序）
     */
    public ShortList getPendingUpdates(long currentTime) {
        ShortList pending = new ShortArrayList();
        
        // 优先处理活跃方块实体
        for (short pos : activePositions) {
            if (needsUpdate(pos, currentTime)) {
                pending.add(pos);
            }
        }
        
        // 如果还有时间预算，处理非活跃方块实体
        if (pending.size() < 100) {
            for (short pos : inactivePositions) {
                if (needsUpdate(pos, currentTime) && !pending.contains(pos)) {
                    pending.add(pos);
                }
            }
        }
        
        // 按距离排序
        pending.sort((a, b) -> {
            int ax = unpackX(a) - playerChunkX;
            int az = unpackZ(a) - playerChunkZ;
            int bx = unpackX(b) - playerChunkX;
            int bz = unpackZ(b) - playerChunkZ;
            return Integer.compare(ax * ax + az * az, bx * bx + bz * bz);
        });
        
        return pending;
    }
    
    /**
     * 从 packed short 解包 X 坐标
     */
    private int unpackX(short packed) {
        return (packed >> 8) & 0xFF;
    }
    
    /**
     * 从 packed short 解包 Z 坐标
     */
    private int unpackZ(short packed) {
        return packed & 0xFF;
    }
    
    /**
     * 跳过更新（用于性能节流）
     */
    public void skipUpdate(short packedPos) {
        updatesSkipped++;
    }
    
    /**
     * 获取活跃方块实体数量
     */
    public int getActiveCount() {
        return activeCount;
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format("Active: %d, Inactive: %d, Skipped: %d", 
            activeCount, inactiveCount, updatesSkipped);
    }
}
