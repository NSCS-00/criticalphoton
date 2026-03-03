package com.dlzstudio.criticalphoton.unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * 零 GC 内存分配器
 * 使用直接内存和对象池，目标零 GC
 */
public class ZeroGCAllocator {
    
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
    
    // 内存池 (固定大小块)
    private final long[] freeList;
    private final int blockSize;
    private final int poolSize;
    private int freeHead;
    
    // 大对象直接内存
    private final long[] largeAllocations;
    private final int maxLargeAllocs;
    private int largeAllocCount;
    
    // 统计
    private long totalAllocations;
    private long totalFrees;
    private long poolHits;
    private long poolMisses;
    
    public ZeroGCAllocator(int poolSize, int blockSize, int maxLargeAllocs) {
        this.poolSize = poolSize;
        this.blockSize = blockSize;
        this.maxLargeAllocs = maxLargeAllocs;
        
        // 初始化内存池
        freeList = new long[poolSize];
        long basePtr = UNSAFE.allocateMemory((long) poolSize * blockSize);
        
        // 将所有块加入空闲列表
        for (int i = 0; i < poolSize; i++) {
            freeList[i] = basePtr + (long) i * blockSize;
        }
        freeHead = 0;
        
        // 初始化大对象分配表
        largeAllocations = new long[maxLargeAllocs];
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            UNSAFE.freeMemory(basePtr);
            for (int i = 0; i < largeAllocCount; i++) {
                if (largeAllocations[i] != 0) {
                    UNSAFE.freeMemory(largeAllocations[i]);
                }
            }
        }));
    }
    
    /**
     * 分配内存 (优先从池中获取)
     */
    public long allocate(int size) {
        totalAllocations++;
        
        if (size <= blockSize) {
            // 从池中分配
            if (freeHead < poolSize) {
                long ptr = freeList[freeHead];
                freeHead++;
                poolHits++;
                return ptr;
            }
            poolMisses++;
        }
        
        // 大对象或池耗尽，直接分配
        if (largeAllocCount < maxLargeAllocs) {
            long ptr = UNSAFE.allocateMemory(size);
            largeAllocations[largeAllocCount++] = ptr;
            return ptr;
        }
        
        throw new OutOfMemoryError("ZeroGCAllocator exhausted");
    }
    
    /**
     * 释放内存 (返回池中)
     */
    public void free(long ptr, int size) {
        totalFrees++;
        
        if (size <= blockSize && freeHead > 0) {
            // 返回池中
            freeHead--;
            freeList[freeHead] = ptr;
            return;
        }
        
        // 大对象，查找并释放
        for (int i = 0; i < largeAllocCount; i++) {
            if (largeAllocations[i] == ptr) {
                UNSAFE.freeMemory(ptr);
                largeAllocations[i] = largeAllocations[--largeAllocCount];
                largeAllocations[largeAllocCount] = 0;
                return;
            }
        }
    }
    
    /**
     * 分配数组 (int[])
     */
    public IntArray allocateIntArray(int length) {
        long ptr = allocate(length * 4 + 4); // 4 字节/元素 + 长度头
        UNSAFE.putInt(ptr, length);
        return new IntArray(ptr, length, this);
    }
    
    /**
     * 分配数组 (float[])
     */
    public FloatArray allocateFloatArray(int length) {
        long ptr = allocate(length * 4 + 4);
        UNSAFE.putInt(ptr, length);
        return new FloatArray(ptr, length, this);
    }
    
    /**
     * 分配数组 (byte[])
     */
    public ByteArray allocateByteArray(int length) {
        long ptr = allocate(length + 4);
        UNSAFE.putInt(ptr, length);
        return new ByteArray(ptr, length, this);
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
            "Allocs: %d, Frees: %d, Pool Hits: %d (%.1f%%), Pool Misses: %d",
            totalAllocations, totalFrees, poolHits,
            totalAllocations > 0 ? (float) poolHits / totalAllocations * 100 : 0,
            poolMisses
        );
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        totalAllocations = 0;
        totalFrees = 0;
        poolHits = 0;
        poolMisses = 0;
    }
}

/**
 * 直接内存 int 数组包装器
 */
class IntArray {
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    
    private final long ptr;
    private final int length;
    private final ZeroGCAllocator allocator;
    
    IntArray(long ptr, int length, ZeroGCAllocator allocator) {
        this.ptr = ptr;
        this.length = length;
        this.allocator = allocator;
    }
    
    public int get(int index) {
        return UNSAFE.getInt(ptr + 4 + (long) index * 4);
    }
    
    public void set(int index, int value) {
        UNSAFE.putInt(ptr + 4 + (long) index * 4, value);
    }
    
    public int length() { return length; }
    
    public void free() {
        allocator.free(ptr, length * 4 + 4);
    }
}

/**
 * 直接内存 float 数组包装器
 */
class FloatArray {
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    
    private final long ptr;
    private final int length;
    private final ZeroGCAllocator allocator;
    
    FloatArray(long ptr, int length, ZeroGCAllocator allocator) {
        this.ptr = ptr;
        this.length = length;
        this.allocator = allocator;
    }
    
    public float get(int index) {
        return UNSAFE.getFloat(ptr + 4 + (long) index * 4);
    }
    
    public void set(int index, float value) {
        UNSAFE.putFloat(ptr + 4 + (long) index * 4, value);
    }
    
    public int length() { return length; }
    
    public void free() {
        allocator.free(ptr, length * 4 + 4);
    }
}

/**
 * 直接内存 byte 数组包装器
 */
class ByteArray {
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    
    private final long ptr;
    private final int length;
    private final ZeroGCAllocator allocator;
    
    ByteArray(long ptr, int length, ZeroGCAllocator allocator) {
        this.ptr = ptr;
        this.length = length;
        this.allocator = allocator;
    }
    
    public byte get(int index) {
        return UNSAFE.getByte(ptr + 4 + (long) index);
    }
    
    public void set(int index, byte value) {
        UNSAFE.putByte(ptr + 4 + (long) index, value);
    }
    
    public int length() { return length; }
    
    public void free() {
        allocator.free(ptr, length + 4);
    }
}
