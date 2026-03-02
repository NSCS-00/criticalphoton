package com.dlzstudio.criticalphoton.network;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网络同步优化器
 * 使用增量更新和优先级队列优化网络同步
 */
public class NetworkSyncOptimizer {
    
    // 实体同步优先级队列
    private final SyncEntry[] syncQueue;
    private final int maxEntries;
    private final AtomicInteger queueSize;
    
    // 上次同步状态
    private final double[] lastPositions;
    private final float[] lastRotations;
    private final byte[] lastStates;
    
    // 同步阈值
    private static final double POSITION_THRESHOLD = 0.015625; // 1/64 方块
    private static final float ROTATION_THRESHOLD = 1.0f;
    
    // 同步间隔（毫秒）
    private static final int SYNC_INTERVAL_HIGH_PRIORITY = 50;
    private static final int SYNC_INTERVAL_LOW_PRIORITY = 200;
    
    public NetworkSyncOptimizer(int maxEntries) {
        this.maxEntries = maxEntries;
        this.syncQueue = new SyncEntry[maxEntries];
        for (int i = 0; i < maxEntries; i++) {
            syncQueue[i] = new SyncEntry();
        }
        this.queueSize = new AtomicInteger(0);
        this.lastPositions = new double[maxEntries * 3];
        this.lastRotations = new float[maxEntries * 2];
        this.lastStates = new byte[maxEntries];
    }
    
    /**
     * 检查是否需要同步位置
     */
    public boolean needsPositionSync(int entityId, double x, double y, double z) {
        int index = entityId % maxEntries;
        int baseIndex = index * 3;
        
        double dx = x - lastPositions[baseIndex];
        double dy = y - lastPositions[baseIndex + 1];
        double dz = z - lastPositions[baseIndex + 2];
        
        return Math.abs(dx) > POSITION_THRESHOLD ||
               Math.abs(dy) > POSITION_THRESHOLD ||
               Math.abs(dz) > POSITION_THRESHOLD;
    }
    
    /**
     * 检查是否需要同步旋转
     */
    public boolean needsRotationSync(int entityId, float yaw, float pitch) {
        int index = entityId % maxEntries;
        int baseIndex = index * 2;
        
        float dyaw = Math.abs(yaw - lastRotations[baseIndex]);
        float dpitch = Math.abs(pitch - lastRotations[baseIndex + 1]);
        
        // 处理角度环绕
        if (dyaw > 180) dyaw = 360 - dyaw;
        if (dpitch > 180) dpitch = 360 - dpitch;
        
        return dyaw > ROTATION_THRESHOLD || dpitch > ROTATION_THRESHOLD;
    }
    
    /**
     * 更新最后同步状态
     */
    public void updateLastState(int entityId, double x, double y, double z, float yaw, float pitch) {
        int index = entityId % maxEntries;
        int posIndex = index * 3;
        int rotIndex = index * 2;
        
        lastPositions[posIndex] = x;
        lastPositions[posIndex + 1] = y;
        lastPositions[posIndex + 2] = z;
        lastRotations[rotIndex] = yaw;
        lastRotations[rotIndex + 1] = pitch;
    }
    
    /**
     * 添加同步条目
     */
    public void addSyncEntry(int entityId, int priority) {
        int size = queueSize.get();
        if (size < maxEntries) {
            SyncEntry entry = syncQueue[size];
            entry.entityId = entityId;
            entry.priority = priority;
            entry.timestamp = System.currentTimeMillis();
            queueSize.incrementAndGet();
        }
    }
    
    /**
     * 获取同步间隔
     */
    public int getSyncInterval(int priority) {
        return priority > 5 ? SYNC_INTERVAL_HIGH_PRIORITY : SYNC_INTERVAL_LOW_PRIORITY;
    }
    
    /**
     * 清除同步队列
     */
    public void clearQueue() {
        queueSize.set(0);
    }
    
    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return queueSize.get();
    }
    
    /**
     * 同步条目
     */
    public static class SyncEntry {
        public int entityId;
        public int priority;
        public long timestamp;
    }
}
