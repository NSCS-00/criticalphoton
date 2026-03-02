package com.dlzstudio.criticalphoton.tick;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tick 系统优化器
 * 使用时间切片和优先级调度优化 Tick 处理
 */
public class TickScheduler {
    
    // Tick 任务队列
    private final TickTask[] taskQueue;
    private final int maxTasks;
    private final AtomicInteger taskCount;
    private final int[] taskPriorities;
    private final long[] taskLastRun;
    private final Runnable[] taskActions;
    
    // Tick 预算（毫秒）
    private static final long TICK_BUDGET = 50; // 50ms = 20 TPS
    private static final long FRAME_BUDGET = 16; // 16ms = 60 FPS
    
    // 当前 Tick 时间
    private long currentTickTime;
    private int ticksProcessed;
    
    // 优先级常量
    public static final int PRIORITY_CRITICAL = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_LOW = 3;
    
    public TickScheduler(int maxTasks) {
        this.maxTasks = maxTasks;
        this.taskQueue = new TickTask[maxTasks];
        for (int i = 0; i < maxTasks; i++) {
            taskQueue[i] = new TickTask();
        }
        this.taskCount = new AtomicInteger(0);
        this.taskPriorities = new int[maxTasks];
        this.taskLastRun = new long[maxTasks];
        this.taskActions = new Runnable[maxTasks];
        this.currentTickTime = 0;
        this.ticksProcessed = 0;
    }
    
    /**
     * 添加 Tick 任务
     */
    public void addTask(Runnable action, int priority, int intervalTicks) {
        int index = taskCount.get();
        if (index < maxTasks) {
            TickTask task = taskQueue[index];
            task.id = index;
            task.priority = priority;
            task.intervalTicks = intervalTicks;
            taskActions[index] = action;
            taskLastRun[index] = 0;
            taskCount.incrementAndGet();
        }
    }
    
    /**
     * 执行 Tick（带时间预算）
     */
    public void executeTick(long currentTime) {
        currentTickTime = currentTime;
        int count = taskCount.get();
        
        // 按优先级排序执行
        for (int priority = PRIORITY_CRITICAL; priority <= PRIORITY_LOW; priority++) {
            long tickStart = System.nanoTime();
            
            for (int i = 0; i < count; i++) {
                if (taskPriorities[i] == priority) {
                    long elapsed = (currentTime - taskLastRun[i]) / 50; // 转换为 Tick 数
                    
                    if (elapsed >= taskQueue[i].intervalTicks) {
                        // 执行任务
                        try {
                            taskActions[i].run();
                            taskLastRun[i] = currentTime;
                            ticksProcessed++;
                        } catch (Exception e) {
                            // 记录错误但不中断其他任务
                        }
                        
                        // 检查时间预算
                        if (System.nanoTime() - tickStart > TICK_BUDGET * 1_000_000) {
                            // 超出预算，剩余任务延迟到下一 Tick
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 执行帧更新（轻量级 Tick）
     */
    public void executeFrameUpdate(long currentTime) {
        int count = taskCount.get();
        long frameStart = System.nanoTime();
        
        for (int i = 0; i < count; i++) {
            if (taskPriorities[i] == PRIORITY_CRITICAL) {
                try {
                    taskActions[i].run();
                } catch (Exception e) {
                    // 忽略
                }
                
                // 检查帧预算
                if (System.nanoTime() - frameStart > FRAME_BUDGET * 1_000_000) {
                    return;
                }
            }
        }
    }
    
    /**
     * 移除任务
     */
    public void removeTask(int taskId) {
        if (taskId >= 0 && taskId < maxTasks) {
            taskActions[taskId] = null;
            taskLastRun[taskId] = 0;
            // 压缩队列
            compressQueue();
        }
    }
    
    /**
     * 压缩任务队列
     */
    private void compressQueue() {
        int writeIndex = 0;
        for (int i = 0; i < maxTasks; i++) {
            if (taskActions[i] != null) {
                if (i != writeIndex) {
                    taskQueue[writeIndex] = taskQueue[i];
                    taskPriorities[writeIndex] = taskPriorities[i];
                    taskLastRun[writeIndex] = taskLastRun[i];
                    taskActions[writeIndex] = taskActions[i];
                }
                writeIndex++;
            }
        }
        taskCount.set(writeIndex);
    }
    
    /**
     * 获取已处理 Tick 数
     */
    public int getTicksProcessed() {
        return ticksProcessed;
    }
    
    /**
     * 获取当前 Tick 时间
     */
    public long getCurrentTickTime() {
        return currentTickTime;
    }
    
    /**
     * 重置 Tick 计数器
     */
    public void resetCounter() {
        ticksProcessed = 0;
    }
    
    /**
     * 获取任务数量
     */
    public int getTaskCount() {
        return taskCount.get();
    }
    
    /**
     * Tick 任务
     */
    public static class TickTask {
        public int id;
        public int priority;
        public int intervalTicks;
    }
}
