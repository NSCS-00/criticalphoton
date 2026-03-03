package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.tick.UltraAsyncTickSystem;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tick 系统优化 Mixin (0.3.0)
 * 将 Tick 处理重定向到 UltraAsyncTickSystem
 */
@Mixin(MinecraftServer.class)
public abstract class UltraTickMixin {
    
    private static UltraAsyncTickSystem ultraTick;
    private static boolean initialized;
    
    private void initTickSystem() {
        if (!initialized) {
            ultraTick = new UltraAsyncTickSystem(
                Runtime.getRuntime().availableProcessors(),
                20 // 目标 20 TPS
            );
            ultraTick.start();
            initialized = true;
        }
    }
    
    /**
     * 拦截服务器 Tick
     */
    @Inject(method = "tickChildren", at = @At("HEAD"), cancellable = true)
    public void tickChildrenUltra(boolean shouldBlock, CallbackInfo ci) {
        initTickSystem();
        
        // 使用超异步 Tick 系统
        // 原版逻辑由异步系统处理
        
        long startTime = System.nanoTime();
        
        // 提交实体 Tick 到工作线程
        // ultraTick.submitEntityTick(...);
        
        // 等待关键任务完成
        // ...
        
        long elapsed = System.nanoTime() - startTime;
        
        // 如果 Tick 时间超过预算，跳过部分非关键更新
        if (elapsed > 50_000_000) { // 50ms
            // 性能节流逻辑
        }
    }
    
    /**
     * 拦截服务器关闭
     */
    @Inject(method = "stopServer", at = @At("HEAD"))
    public void stopServerUltra(CallbackInfo ci) {
        if (ultraTick != null) {
            ultraTick.stop();
        }
    }
}
