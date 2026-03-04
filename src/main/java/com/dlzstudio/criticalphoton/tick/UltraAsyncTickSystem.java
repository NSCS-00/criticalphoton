package com.dlzstudio.criticalphoton.tick;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 完全异步 Tick 系统
 * 将所有非关键逻辑移到工作线程
 * 目标：主线程 Tick 时间 < 5ms
 */
public class UltraAsyncTickSystem {
    
    // 工作线程池
    private final ExecutorService tickExecutor;
    private final ExecutorService ioExecutor;
    private final ExecutorService computeExecutor;
    
    // 任务队列 (无锁)
    private final ConcurrentLinkedQueue<Runnable> tickQueue;
    private final ConcurrentLinkedQueue<Runnable> ioQueue;
    private final ConcurrentLinkedQueue<Runnable> computeQueue;
    
    // Tick 调度器
    private final AtomicInteger tickCounter;
    private final AtomicLong lastTickTime;
    private final AtomicLong totalTickTime;
    
    // 配置
    private final int targetTPS;
    private final long tickIntervalNanos;
    private final int numWorkers;
    
    // 运行状态
    private volatile boolean running;
    private Thread tickThread;
    
    // 性能统计
    private int ticksProcessed;
    private int tasksProcessed;
    private long maxTickTime;
    
    public UltraAsyncTickSystem(int numWorkers, int targetTPS) {
        this.numWorkers = numWorkers;
        this.targetTPS = targetTPS;
        this.tickIntervalNanos = 1_000_000_000L / targetTPS;
        
        // 创建工作线程池
        this.tickExecutor = Executors.newFixedThreadPool(numWorkers, r -> {
            Thread t = new Thread(r, "UltraTick-Worker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        });
        
        this.ioExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "UltraTick-IO");
            t.setDaemon(true);
            return t;
        });
        
        this.computeExecutor = Executors.newFixedThreadPool(numWorkers, r -> {
            Thread t = new Thread(r, "UltraTick-Compute");
            t.setDaemon(true);
            return t;
        });
        
        // 初始化队列
        this.tickQueue = new ConcurrentLinkedQueue<>();
        this.ioQueue = new ConcurrentLinkedQueue<>();
        this.computeQueue = new ConcurrentLinkedQueue<>();
        
        this.tickCounter = new AtomicInteger(0);
        this.lastTickTime = new AtomicLong(System.nanoTime());
        this.totalTickTime = new AtomicLong(0);
        
        this.running = false;
    }
    
    /**
     * 启动 Tick 系统
     */
    public void start() {
        if (running) return;
        running = true;
        
        tickThread = new Thread(this::tickLoop, "UltraTick-Main");
        tickThread.setDaemon(true);
        tickThread.setPriority(Thread.MAX_PRIORITY);
        tickThread.start();
    }
    
    /**
     * 停止 Tick 系统
     */
    public void stop() {
        running = false;
        tickExecutor.shutdown();
        ioExecutor.shutdown();
        computeExecutor.shutdown();
        
        try {
            tickExecutor.awaitTermination(5, TimeUnit.SECONDS);
            ioExecutor.awaitTermination(5, TimeUnit.SECONDS);
            computeExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Tick 循环
     */
    private void tickLoop() {
        long nextTickTime = System.nanoTime();
        
        while (running) {
            long startTime = System.nanoTime();
            
            // 等待下一 Tick
            while (System.nanoTime() < nextTickTime) {
                Thread.yield();
            }
            nextTickTime += tickIntervalNanos;
            
            // 处理 Tick 队列
            processTickQueue();
            
            // 统计
            long tickDuration = System.nanoTime() - startTime;
            totalTickTime.addAndGet(tickDuration);
            ticksProcessed++;
            
            if (tickDuration > maxTickTime) {
                maxTickTime = tickDuration;
            }
        }
    }
    
    /**
     * 处理 Tick 队列
     */
    private void processTickQueue() {
        int currentTick = tickCounter.incrementAndGet();
        
        // 批量处理任务
        int batchSize = 0;
        Runnable task;
        
        while ((task = tickQueue.poll()) != null && batchSize < 100) {
            try {
                task.run();
                tasksProcessed++;
                batchSize++;
            } catch (Exception e) {
                // 记录错误但不中断
            }
        }
        
        // 提交并行任务到工作线程
        processParallelTasks(currentTick);
    }
    
    /**
     * 处理并行任务
     */
    private void processParallelTasks(int tick) {
        // 从计算队列获取任务
        Runnable computeTask;
        while ((computeTask = computeQueue.poll()) != null) {
            computeExecutor.submit(computeTask);
        }
        
        // 从 IO 队列获取任务
        Runnable ioTask;
        while ((ioTask = ioQueue.poll()) != null) {
            ioExecutor.submit(ioTask);
        }
    }
    
    /**
     * 添加 Tick 任务
     */
    public void addTickTask(Runnable task) {
        tickQueue.offer(task);
    }
    
    /**
     * 添加 IO 任务 (异步)
     */
    public void addIOTask(Runnable task) {
        ioQueue.offer(task);
    }
    
    /**
     * 添加计算任务 (并行)
     */
    public void addComputeTask(Runnable task) {
        computeQueue.offer(task);
    }
    
    /**
     * 提交实体 Tick 任务 (并行处理)
     */
    public void submitEntityTick(Runnable[] entityTasks, int count) {
        int perWorker = (count + numWorkers - 1) / numWorkers;
        
        for (int w = 0; w < numWorkers; w++) {
            final int start = w * perWorker;
            final int end = Math.min(start + perWorker, count);
            
            if (start >= count) break;
            
            computeExecutor.submit(() -> {
                for (int i = start; i < end; i++) {
                    try {
                        entityTasks[i].run();
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            });
        }
    }
    
    /**
     * 获取平均 Tick 时间 (毫秒)
     */
    public double getAverageTickTime() {
        int ticks = ticksProcessed;
        if (ticks == 0) return 0;
        return (totalTickTime.get() / ticks) / 1_000_000.0;
    }
    
    /**
     * 获取最大 Tick 时间 (毫秒)
     */
    public double getMaxTickTime() {
        return maxTickTime / 1_000_000.0;
    }
    
    /**
     * 获取已处理 Tick 数
     */
    public int getTicksProcessed() {
        return ticksProcessed;
    }
    
    /**
     * 获取已处理任务数
     */
    public int getTasksProcessed() {
        return tasksProcessed;
    }
    
    /**
     * 获取队列大小
     */
    public int getTickQueueSize() {
        return tickQueue.size();
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        ticksProcessed = 0;
        tasksProcessed = 0;
        maxTickTime = 0;
        totalTickTime.set(0);
    }
}
