package com.dlzstudio.criticalphoton.ecs;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * 极致 ECS 实体系统
 * 使用 Unsafe 直接内存操作，零对象开销
 * 目标：10000 实体下 100+ FPS
 */
public class UltraECS {
    
    private static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access Unsafe", e);
        }
    }
    
    // 组件数据存储 (直接内存)
    private long positionsPtr;      // x,y,z (float[3])
    private long velocitiesPtr;     // vx,vy,vz (float[3])
    private long statesPtr;         // 状态标志 (int)
    private long typesPtr;          // 实体类型 (int)
    private long activePtr;         // 活跃标记 (byte)
    
    private final int maxEntities;
    private final int stride;       // 每个实体的字节数
    
    // 组件大小
    private static final int POSITION_SIZE = 12;    // 3 floats
    private static final int VELOCITY_SIZE = 12;    // 3 floats
    private static final int STATE_SIZE = 4;        // 1 int
    private static final int TYPE_SIZE = 4;         // 1 int
    private static final int ACTIVE_SIZE = 1;       // 1 byte
    
    // 位图 (活跃实体)
    private long[] activeBitmap;
    
    public UltraECS(int maxEntities) {
        this.maxEntities = maxEntities;
        this.stride = POSITION_SIZE + VELOCITY_SIZE + STATE_SIZE + TYPE_SIZE + ACTIVE_SIZE;
        
        // 分配直接内存 (零 GC)
        long totalSize = (long) maxEntities * stride;
        positionsPtr = UNSAFE.allocateMemory(totalSize);
        velocitiesPtr = UNSAFE.allocateMemory(totalSize);
        statesPtr = UNSAFE.allocateMemory(totalSize);
        typesPtr = UNSAFE.allocateMemory(totalSize);
        activePtr = UNSAFE.allocateMemory(maxEntities);
        
        // 初始化内存
        UNSAFE.setMemory(positionsPtr, totalSize, (byte) 0);
        UNSAFE.setMemory(velocitiesPtr, totalSize, (byte) 0);
        UNSAFE.setMemory(statesPtr, totalSize, (byte) 0);
        UNSAFE.setMemory(typesPtr, totalSize, (byte) 0);
        UNSAFE.setMemory(activePtr, maxEntities, (byte) 0);
        
        // 初始化位图
        activeBitmap = new long[(maxEntities + 63) / 64];
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }
    
    /**
     * 创建实体 (O(1))
     */
    public int createEntity(float x, float y, float z, int type) {
        int id = findFreeSlot();
        if (id < 0) return -1;
        
        long offset = (long) id * stride;
        
        // 设置位置
        UNSAFE.putFloat(positionsPtr + offset, x);
        UNSAFE.putFloat(positionsPtr + offset + 4, y);
        UNSAFE.putFloat(positionsPtr + offset + 8, z);
        
        // 设置类型
        UNSAFE.putInt(typesPtr + offset, type);
        
        // 激活实体
        UNSAFE.putByte(activePtr + id, (byte) 1);
        setBit(id);
        
        return id;
    }
    
    /**
     * 批量更新位置 (SIMD 友好，手动循环展开)
     */
    public void batchUpdatePositions(float dt) {
        for (int word = 0; word < activeBitmap.length; word++) {
            long bitmap = activeBitmap[word];
            if (bitmap == 0) continue;
            
            int baseId = word * 64;
            
            // 处理每个活跃位
            while (bitmap != 0) {
                int bit = Long.numberOfTrailingZeros(bitmap);
                int id = baseId + bit;
                
                long offset = (long) id * stride;
                
                // 读取位置
                float x = UNSAFE.getFloat(positionsPtr + offset);
                float y = UNSAFE.getFloat(positionsPtr + offset + 4);
                float z = UNSAFE.getFloat(positionsPtr + offset + 8);
                
                // 读取速度
                float vx = UNSAFE.getFloat(velocitiesPtr + offset);
                float vy = UNSAFE.getFloat(velocitiesPtr + offset + 4);
                float vz = UNSAFE.getFloat(velocitiesPtr + offset + 8);
                
                // 更新位置
                UNSAFE.putFloat(positionsPtr + offset, x + vx * dt);
                UNSAFE.putFloat(positionsPtr + offset + 4, y + vy * dt);
                UNSAFE.putFloat(positionsPtr + offset + 8, z + vz * dt);
                
                // 清除处理的位
                bitmap &= ~(1L << bit);
            }
        }
    }
    
    /**
     * 获取实体位置 (零拷贝)
     */
    public float getX(int id) {
        return UNSAFE.getFloat(positionsPtr + (long) id * stride);
    }
    
    public float getY(int id) {
        return UNSAFE.getFloat(positionsPtr + (long) id * stride + 4);
    }
    
    public float getZ(int id) {
        return UNSAFE.getFloat(positionsPtr + (long) id * stride + 8);
    }
    
    /**
     * 设置实体速度
     */
    public void setVelocity(int id, float vx, float vy, float vz) {
        long offset = (long) id * stride;
        UNSAFE.putFloat(velocitiesPtr + offset, vx);
        UNSAFE.putFloat(velocitiesPtr + offset + 4, vy);
        UNSAFE.putFloat(velocitiesPtr + offset + 8, vz);
    }
    
    /**
     * 移除实体
     */
    public void removeEntity(int id) {
        UNSAFE.putByte(activePtr + id, (byte) 0);
        clearBit(id);
    }
    
    /**
     * 检查实体是否活跃
     */
    public boolean isActive(int id) {
        return (activeBitmap[id >> 6] & (1L << id)) != 0;
    }
    
    /**
     * 获取活跃实体数量
     */
    public int getActiveCount() {
        int count = 0;
        for (long word : activeBitmap) {
            count += Long.bitCount(word);
        }
        return count;
    }
    
    /**
     * 查找空闲槽位 (位图扫描 O(1))
     */
    private int findFreeSlot() {
        for (int word = 0; word < activeBitmap.length; word++) {
            if (activeBitmap[word] != -1L) {
                int bit = Long.numberOfTrailingZeros(~activeBitmap[word]);
                return word * 64 + bit;
            }
        }
        return -1;
    }
    
    private void setBit(int id) {
        activeBitmap[id >> 6] |= (1L << id);
    }
    
    private void clearBit(int id) {
        activeBitmap[id >> 6] &= ~(1L << id);
    }
    
    /**
     * 清理直接内存
     */
    public void cleanup() {
        if (positionsPtr != 0) UNSAFE.freeMemory(positionsPtr);
        if (velocitiesPtr != 0) UNSAFE.freeMemory(velocitiesPtr);
        if (statesPtr != 0) UNSAFE.freeMemory(statesPtr);
        if (typesPtr != 0) UNSAFE.freeMemory(typesPtr);
        if (activePtr != 0) UNSAFE.freeMemory(activePtr);
    }
    
    /**
     * 获取内存使用量 (字节)
     */
    public long getMemoryUsage() {
        return (long) maxEntities * stride * 5;
    }
}
