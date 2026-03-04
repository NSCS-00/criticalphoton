package com.dlzstudio.criticalphoton.renderer;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AsyncRenderPipeline {
    private static final Logger LOGGER = CriticalPhoton.LOGGER;
    private final ExecutorService renderExecutor;
    private final ExecutorService collectExecutor;
    private final List<Entity>[] frontBuffer;
    private final List<Entity>[] backBuffer;
    private final Object bufferLock = new Object();
    private volatile long lastFrameTime, collectTime, sortTime;
    private boolean initialized;
    private final int maxRenderThreads;
    private final boolean enableAsyncRender;
    
    @SuppressWarnings("unchecked")
    public AsyncRenderPipeline() {
        this.maxRenderThreads = CriticalPhotonConfig.CLIENT.maxRenderThreads.get();
        this.enableAsyncRender = CriticalPhotonConfig.CLIENT.enableOptimizations.get();
        this.renderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CriticalPhoton-RenderThread");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        this.collectExecutor = Executors.newFixedThreadPool(maxRenderThreads, r -> {
            Thread t = new Thread(r, "CriticalPhoton-Collect");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        this.frontBuffer = new ArrayList[3];
        this.backBuffer = new ArrayList[3];
        for (int i = 0; i < 3; i++) {
            frontBuffer[i] = new ArrayList<>();
            backBuffer[i] = new ArrayList<>();
        }
    }
    
    public void initialize() {
        if (initialized) return;
        LOGGER.info("异步渲染管线初始化 - 线程数：{}", maxRenderThreads);
        this.initialized = true;
        this.lastFrameTime = System.nanoTime();
    }
    
    public void update() {
        if (!initialized || !enableAsyncRender) return;
        long frameStart = System.nanoTime();
        
        long collectStart = System.nanoTime();
        collectVisibleEntities();
        collectTime = System.nanoTime() - collectStart;
        
        sortTime = 0;
        lastFrameTime = System.nanoTime() - frameStart;
    }
    
    private void collectVisibleEntities() {
        for (List<Entity> buffer : backBuffer) buffer.clear();
        var dependencyManager = CriticalPhoton.getInstance().getDependencyManager();
        var visibleEntities = dependencyManager.getDependencyGraph().getPotentiallyVisibleEntities();
        
        List<Future<?>> futures = new ArrayList<>();
        List<List<Entity>> batches = partitionEntities(visibleEntities, maxRenderThreads);
        
        for (int i = 0; i < batches.size(); i++) {
            List<Entity> batch = batches.get(i);
            int bufferIndex = i % 3;
            futures.add(collectExecutor.submit(() -> {
                for (Entity entity : batch) {
                    if (dependencyManager.shouldRender(entity)) {
                        synchronized (backBuffer[bufferIndex]) {
                            backBuffer[bufferIndex].add(entity);
                        }
                    }
                }
            }));
        }
        
        for (Future<?> future : futures) {
            try { future.get(100, TimeUnit.MILLISECONDS); }
            catch (Exception e) { LOGGER.warn("实体收集任务超时", e); }
        }
        swapBuffers();
    }
    
    private List<List<Entity>> partitionEntities(java.util.Set<Entity> entities, int numPartitions) {
        List<List<Entity>> partitions = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++) partitions.add(new ArrayList<>());
        int index = 0;
        for (Entity entity : entities) partitions.get(index % numPartitions).add(entity);
        return partitions;
    }
    
    private void swapBuffers() {
        synchronized (bufferLock) {
            for (int i = 0; i < 3; i++) {
                List<Entity> temp = frontBuffer[i];
                frontBuffer[i] = backBuffer[i];
                backBuffer[i] = temp;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<Entity>[] getFrontBuffer() {
        synchronized (bufferLock) { return frontBuffer.clone(); }
    }
    
    public long getLastFrameTime() { return lastFrameTime; }
    public long getCollectTime() { return collectTime; }
    public long getSortTime() { return sortTime; }
    
    public void shutdown() {
        renderExecutor.shutdown();
        collectExecutor.shutdown();
    }
}
