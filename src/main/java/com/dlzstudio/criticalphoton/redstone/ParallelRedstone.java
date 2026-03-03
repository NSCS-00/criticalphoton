package com.dlzstudio.criticalphoton.redstone;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * 并行红石计算系统
 * 使用位压缩和并行计算优化红石更新
 * 目标：5000 高频红石下 CPU 占用 < 10%
 */
public class ParallelRedstone {
    
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
    
    // 红石状态存储 (位压缩：每方块 4 位)
    private long[] redstoneState;     // 信号强度 (0-15)
    private long[] redstonePower;     // 充能状态 (1 位/方块)
    private long[] redstoneUpdate;    // 待更新标记 (1 位/方块)
    
    // 直接内存版本 (用于超大规模)
    private long directStatePtr;
    private long directPowerPtr;
    private long directUpdatePtr;
    
    private final int maxBlocks;
    private final int arraySize;
    private final boolean useDirectMemory;
    
    // 更新队列 (环形缓冲)
    private int[] updateQueue;
    private int queueHead;
    private int queueTail;
    private int queueSize;
    
    // 线程局部工作区
    private final ThreadLocal<RedstoneWorker> workers;
    
    public ParallelRedstone(int maxBlocks, boolean useDirectMemory) {
        this.maxBlocks = maxBlocks;
        this.arraySize = (maxBlocks + 63) / 64;
        this.useDirectMemory = useDirectMemory;
        
        if (useDirectMemory) {
            long bytes = (long) arraySize * 8;
            directStatePtr = UNSAFE.allocateMemory(bytes);
            directPowerPtr = UNSAFE.allocateMemory(bytes);
            directUpdatePtr = UNSAFE.allocateMemory(bytes);
            UNSAFE.setMemory(directStatePtr, bytes, (byte) 0);
            UNSAFE.setMemory(directPowerPtr, bytes, (byte) 0);
            UNSAFE.setMemory(directUpdatePtr, bytes, (byte) 0);
        } else {
            redstoneState = new long[arraySize];
            redstonePower = new long[arraySize];
            redstoneUpdate = new long[arraySize];
        }
        
        // 更新队列 (1024 容量)
        updateQueue = new int[1024];
        queueHead = 0;
        queueTail = 0;
        queueSize = 0;
        
        // 初始化线程工作区
        workers = ThreadLocal.withInitial(RedstoneWorker::new);
    }
    
    /**
     * 设置红石信号强度
     */
    public void setSignal(int x, int y, int z, int strength) {
        int index = hashPosition(x, y, z);
        int arrayIndex = index >> 6;
        int bitIndex = (index & 63) << 2;
        
        if (useDirectMemory) {
            long state = UNSAFE.getLong(directStatePtr + arrayIndex * 8);
            state &= ~(0xFL << bitIndex);
            state |= ((long) strength & 0xF) << bitIndex;
            UNSAFE.putLong(directStatePtr + arrayIndex * 8, state);
        } else {
            redstoneState[arrayIndex] &= ~(0xFL << bitIndex);
            redstoneState[arrayIndex] |= ((long) strength & 0xF) << bitIndex;
        }
        
        // 添加到更新队列
        addToQueue(x, y, z);
    }
    
    /**
     * 获取红石信号强度
     */
    public int getSignal(int x, int y, int z) {
        int index = hashPosition(x, y, z);
        int arrayIndex = index >> 6;
        int bitIndex = (index & 63) << 2;
        
        long state;
        if (useDirectMemory) {
            state = UNSAFE.getLong(directStatePtr + arrayIndex * 8);
        } else {
            state = redstoneState[arrayIndex];
        }
        
        return (int) ((state >>> bitIndex) & 0xF);
    }
    
    /**
     * 设置充能状态
     */
    public void setPowered(int x, int y, int z, boolean powered) {
        int index = hashPosition(x, y, z);
        int arrayIndex = index >> 6;
        int bitIndex = index & 63;
        
        if (useDirectMemory) {
            long power = UNSAFE.getLong(directPowerPtr + arrayIndex * 8);
            if (powered) {
                power |= (1L << bitIndex);
            } else {
                power &= ~(1L << bitIndex);
            }
            UNSAFE.putLong(directPowerPtr + arrayIndex * 8, power);
        } else {
            if (powered) {
                redstonePower[arrayIndex] |= (1L << bitIndex);
            } else {
                redstonePower[arrayIndex] &= ~(1L << bitIndex);
            }
        }
    }
    
    /**
     * 并行更新所有红石 (使用所有 CPU 核心)
     */
    public void parallelUpdate() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[numThreads];
        
        int range = arraySize / numThreads;
        
        for (int t = 0; t < numThreads; t++) {
            final int start = t * range;
            final int end = (t == numThreads - 1) ? arraySize : start + range;
            
            threads[t] = new Thread(() -> {
                RedstoneWorker worker = workers.get();
                worker.processRange(start, end);
            });
            threads[t].setName("Redstone-Worker-" + t);
            threads[t].setDaemon(true);
            threads[t].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 添加到更新队列
     */
    private void addToQueue(int x, int y, int z) {
        if (queueSize < updateQueue.length) {
            int packed = packPosition(x, y, z);
            updateQueue[queueTail] = packed;
            queueTail = (queueTail + 1) & (updateQueue.length - 1);
            queueSize++;
        }
    }
    
    /**
     * 处理更新队列
     */
    public void processQueue() {
        while (queueSize > 0) {
            int packed = updateQueue[queueHead];
            queueHead = (queueHead + 1) & (updateQueue.length - 1);
            queueSize--;
            
            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);
            
            propagateSignal(x, y, z);
        }
    }
    
    /**
     * 传播信号 (简化版)
     */
    private void propagateSignal(int x, int y, int z) {
        int currentStrength = getSignal(x, y, z);
        if (currentStrength <= 0) return;
        
        // 向 6 个方向传播
        int newStrength = currentStrength - 1;
        setSignalIfHigher(x + 1, y, z, newStrength);
        setSignalIfHigher(x - 1, y, z, newStrength);
        setSignalIfHigher(x, y + 1, z, newStrength);
        setSignalIfHigher(x, y - 1, z, newStrength);
        setSignalIfHigher(x, y, z + 1, newStrength);
        setSignalIfHigher(x, y, z - 1, newStrength);
    }
    
    private void setSignalIfHigher(int x, int y, int z, int strength) {
        int current = getSignal(x, y, z);
        if (strength > current) {
            setSignal(x, y, z, strength);
        }
    }
    
    /**
     * 哈希位置到索引
     */
    private int hashPosition(int x, int y, int z) {
        int hash = x * 31337 + y * 8192 + z * 65537;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        return hash & 0x7FFFFFFF;
    }
    
    private int packPosition(int x, int y, int z) {
        return ((x & 0xFFF) << 20) | ((y & 0xFFF) << 8) | (z & 0xFF);
    }
    
    private int unpackX(int packed) { return (packed >>> 20) & 0xFFF; }
    private int unpackY(int packed) { return (packed >>> 8) & 0xFFF; }
    private int unpackZ(int packed) { return packed & 0xFF; }
    
    /**
     * 清理直接内存
     */
    public void cleanup() {
        if (useDirectMemory) {
            UNSAFE.freeMemory(directStatePtr);
            UNSAFE.freeMemory(directPowerPtr);
            UNSAFE.freeMemory(directUpdatePtr);
        }
    }
    
    /**
     * 线程局部工作区
     */
    private class RedstoneWorker {
        public void processRange(int start, int end) {
            for (int i = start; i < end; i++) {
                // 处理每个 64 位块
                long state, power;
                if (useDirectMemory) {
                    state = UNSAFE.getLong(directStatePtr + i * 8);
                    power = UNSAFE.getLong(directPowerPtr + i * 8);
                } else {
                    state = redstoneState[i];
                    power = redstonePower[i];
                }
                
                // 位级并行处理
                // ...
            }
        }
    }
}
