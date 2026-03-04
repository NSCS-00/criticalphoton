package com.dlzstudio.criticalphoton.system;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayDeque;
import java.util.Deque;

public class PerformanceMonitor {
    private static final Logger LOGGER = CriticalPhoton.LOGGER;
    private final Deque<Long> frameTimes;
    private final FrameMetrics frameMetrics;
    private long usedMemory, maxMemory;
    private final ThreadMXBean threadBean;
    private long lastCpuTime;
    private int fps, frameCount;
    private long lastFpsUpdate;
    private final PerformanceThrottler throttler;
    private boolean enabled;
    
    public PerformanceMonitor() {
        this.frameTimes = new ArrayDeque<>(120);
        this.frameMetrics = new FrameMetrics();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.throttler = new PerformanceThrottler();
        this.enabled = true;
    }
    
    public void start() {
        enabled = true;
        lastFpsUpdate = System.currentTimeMillis();
        lastCpuTime = threadBean.getCurrentThreadCpuTime();
        LOGGER.info("性能监控已启动");
    }
    
    public void update() {
        if (!enabled) return;
        long currentTime = System.currentTimeMillis();
        frameTimes.addLast(currentTime - lastFpsUpdate);
        if (frameTimes.size() > 120) frameTimes.removeFirst();
        
        frameCount++;
        if (currentTime - lastFpsUpdate >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsUpdate = currentTime;
            updateMemoryStats();
            throttler.update(fps, frameMetrics);
        }
        long currentCpuTime = threadBean.getCurrentThreadCpuTime();
        frameMetrics.setCpuTime(currentCpuTime - lastCpuTime);
        lastCpuTime = currentCpuTime;
    }
    
    public void recordPhaseTime(Phase phase, long timeNanos) { frameMetrics.setPhaseTime(phase, timeNanos); }
    
    private void updateMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        usedMemory = runtime.totalMemory() - runtime.freeMemory();
        maxMemory = runtime.maxMemory();
    }
    
    public int getFps() { return fps; }
    public double getAverageFrameTime() {
        if (frameTimes.isEmpty()) return 0;
        return frameTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
    }
    public double getMemoryUsage() { return (double) usedMemory / maxMemory; }
    public long getUsedMemoryMB() { return usedMemory / (1024 * 1024); }
    public long getMaxMemoryMB() { return maxMemory / (1024 * 1024); }
    public FrameMetrics getFrameMetrics() { return frameMetrics; }
    public PerformanceThrottler getThrottler() { return throttler; }
    
    public enum Phase {
        RENDER_SETUP, ENTITY_COLLECT, ENTITY_SORT, ENTITY_RENDER,
        BLOCK_RENDER, TILE_ENTITY, PARTICLE, GUI, TOTAL
    }
    
    public static class FrameMetrics {
        private final long[] phaseTimes;
        private long cpuTime;
        public FrameMetrics() { this.phaseTimes = new long[Phase.values().length]; }
        public void setPhaseTime(Phase phase, long timeNanos) { phaseTimes[phase.ordinal()] = timeNanos; }
        public long getPhaseTime(Phase phase) { return phaseTimes[phase.ordinal()]; }
        public void setCpuTime(long timeNanos) { this.cpuTime = timeNanos; }
        public long getCpuTime() { return cpuTime; }
        public double getTotalRenderTimeMs() {
            long total = 0; for (long time : phaseTimes) total += time;
            return total / 1_000_000.0;
        }
        public double getPhaseTimeMs(Phase phase) { return phaseTimes[phase.ordinal()] / 1_000_000.0; }
    }
}
