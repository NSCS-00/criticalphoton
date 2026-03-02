package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.tick.TickScheduler;
import com.dlzstudio.criticalphoton.tick.AsyncTickHandler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tick 系统优化 Mixin
 */
@Mixin(MinecraftServer.class)
public abstract class TickOptimizerMixin {
    
    private static TickScheduler tickScheduler;
    private static AsyncTickHandler asyncTickHandler;
    private static boolean initialized;
    
    private void initTickSystem() {
        if (!initialized) {
            tickScheduler = new TickScheduler(256);
            asyncTickHandler = new AsyncTickHandler(50, 1024);
            asyncTickHandler.start();
            initialized = true;
        }
    }
    
    /**
     * 优化服务器 Tick
     */
    @Inject(method = "tickChildren", at = @At("HEAD"))
    public void tickChildrenOptimized(boolean shouldBlock, CallbackInfo ci) {
        initTickSystem();
        
        long currentTime = System.currentTimeMillis();
        tickScheduler.executeTick(currentTime);
    }
    
    /**
     * 注入异步 Tick 任务
     */
    @Inject(method = "tickChildren", at = @At("TAIL"))
    public void scheduleAsyncTicks(boolean shouldBlock, CallbackInfo ci) {
        if (asyncTickHandler != null) {
            // 将非关键任务调度到异步处理器
            asyncTickHandler.addTask(() -> {
                // 异步处理逻辑
            });
        }
    }
}
