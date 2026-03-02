package com.dlzstudio.criticalphoton.renderer;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步渲染管线
 */
public class AsyncRenderPipeline {
    
    private static final Logger LOGGER = CriticalPhoton.LOGGER;
    
    private final ExecutorService renderExecutor;
    private final ExecutorService collectExecutor;
    private final BlockingQueue<RenderCommand> renderQueue;
    private final List<Entity>[] frontBuffer;
    private final List<Entity>[] backBuffer;
    private final Object bufferLock = new Object();
    private final AtomicInteger frameCounter;
    private volatile long lastFrameTime;
    private volatile long collectTime;
    private volatile long sortTime;
    private volatile long renderTime;
    private boolean initialized;
    private final int maxRenderThreads;
    private final boolean enableAsyncRender;
    
    @SuppressWarnings("unchecked")
    public AsyncRenderPipeline() {
        this.maxRenderThreads = CriticalPhotonConfig.CLIENT.maxRenderThreads.get();
        this.enableAsyncRender = CriticalPhotonConfig.CLIENT.enableAsyncRender.get();
        
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
        
        this.renderQueue = new LinkedBlockingQueue<>(10000);
        this.frontBuffer = new ArrayList[3];
        this.backBuffer = new ArrayList[3];
        for (int i = 0; i < 3; i++) {
            frontBuffer[i] = new ArrayList<>();
            backBuffer[i] = new ArrayList<>();
        }
        this.frameCounter = new AtomicInteger(0);
        this.initialized = false;
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
        
        long sortStart = System.nanoTime();
        sortRenderCommands();
        sortTime = System.nanoTime() - sortStart;
        
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
    
    private void sortRenderCommands() {
        while (!renderQueue.isEmpty()) {
            RenderCommand command = renderQueue.poll();
            if (command != null) command.markSorted();
        }
    }
    
    public void submitRenderCommand(RenderCommand command) {
        if (renderQueue.offer(command)) command.submitTime = System.nanoTime();
    }
    
    public void executeRenderCommands() {
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return;
        
        long renderStart = System.nanoTime();
        List<RenderCommand> commands = new ArrayList<>();
        renderQueue.drainTo(commands);
        
        for (RenderCommand command : commands) {
            try { command.execute(); }
            catch (Exception e) { LOGGER.error("执行渲染命令失败", e); }
        }
        renderTime = System.nanoTime() - renderStart;
    }
    
    @SuppressWarnings("unchecked")
    public List<Entity>[] getFrontBuffer() {
        synchronized (bufferLock) { return frontBuffer.clone(); }
    }
    
    public int getCurrentFrame() { return frameCounter.getAndIncrement(); }
    public long getCollectTime() { return collectTime; }
    public long getSortTime() { return sortTime; }
    public long getRenderTime() { return renderTime; }
    public long getLastFrameTime() { return lastFrameTime; }
    
    public void shutdown() {
        renderExecutor.shutdown();
        collectExecutor.shutdown();
        try {
            if (!renderExecutor.awaitTermination(5, TimeUnit.SECONDS)) renderExecutor.shutdownNow();
            if (!collectExecutor.awaitTermination(5, TimeUnit.SECONDS)) collectExecutor.shutdownNow();
        } catch (InterruptedException e) {
            renderExecutor.shutdownNow();
            collectExecutor.shutdownNow();
        }
    }
}
