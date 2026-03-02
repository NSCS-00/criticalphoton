package com.dlzstudio.criticalphoton.tick;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步 Tick 处理器
 * 在后台线程处理非关键 Tick 逻辑
 */
public class AsyncTickHandler {
    
    // 工作线程
    private final Thread workerThread;
    private volatile boolean running;
    private volatile boolean paused;
    
    // Tick 间隔（毫秒）
    private final long tickInterval;
    
    // Tick 计数器
    private final AtomicLong tickCount;
    private final AtomicLong skippedTicks;
    
    // 任务队列
    private final Runnable[] taskQueue;
    private final int maxTasks;
    private int queueHead;
    private int queueTail;
    
    public AsyncTickHandler(long tickInterval, int maxTasks) {
        this.tickInterval = tickInterval;
        this.maxTasks = maxTasks;
        this.taskQueue = new Runnable[maxTasks];
        this.tickCount = new AtomicLong(0);
        this.skippedTicks = new AtomicLong(0);
        this.running = false;
        this.paused = false;
        
        this.workerThread = new Thread(this::tickLoop, "AsyncTickHandler");
        this.workerThread.setDaemon(true);
        this.workerThread.setPriority(Thread.NORM_PRIORITY - 1);
    }
    
    /**
     * Tick 循环
     */
    private void tickLoop() {
        long lastTickTime = System.currentTimeMillis();
        
        while (running) {
            if (paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastTickTime;
            
            if (elapsed >= tickInterval) {
                // 执行 Tick
                processTick();
                tickCount.incrementAndGet();
                lastTickTime = currentTime;
                
                // 检查是否跳过 Tick
                if (elapsed > tickInterval * 2) {
                    skippedTicks.addAndGet(elapsed / tickInterval - 1);
                }
            } else {
                // 等待下一 Tick
                try {
                    Thread.sleep(tickInterval - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 处理 Tick
     */
    private void processTick() {
        // 处理任务队列
        while (queueHead != queueTail) {
            Runnable task = taskQueue[queueHead];
            queueHead = (queueHead + 1) % maxTasks;
            
            try {
                task.run();
            } catch (Exception e) {
                // 记录错误但不中断
            }
        }
    }
    
    /**
     * 添加异步任务
     */
    public void addTask(Runnable task) {
        int nextTail = (queueTail + 1) % maxTasks;
        if (nextTail != queueHead) {
            taskQueue[queueTail] = task;
            queueTail = nextTail;
        }
    }
    
    /**
     * 启动处理器
     */
    public void start() {
        if (!running) {
            running = true;
            workerThread.start();
        }
    }
    
    /**
     * 停止处理器
     */
    public void stop() {
        running = false;
        workerThread.interrupt();
        try {
            workerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 暂停处理
     */
    public void pause() {
        paused = true;
    }
    
    /**
     * 恢复处理
     */
    public void resume() {
        paused = false;
    }
    
    /**
     * 获取 Tick 计数
     */
    public long getTickCount() {
        return tickCount.get();
    }
    
    /**
     * 获取跳过 Tick 数
     */
    public long getSkippedTicks() {
        return skippedTicks.get();
    }
    
    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        if (queueTail >= queueHead) {
            return queueTail - queueHead;
        } else {
            return maxTasks - queueHead + queueTail;
        }
    }
}
