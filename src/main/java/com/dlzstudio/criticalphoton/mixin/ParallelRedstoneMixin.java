package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.redstone.ParallelRedstone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 红石系统优化 Mixin (0.3.0)
 * 将红石更新重定向到 ParallelRedstone
 */
@Mixin(Level.class)
public abstract class ParallelRedstoneMixin {
    
    private static ParallelRedstone parallelRedstone;
    private static boolean initialized;
    
    private void initRedstone() {
        if (!initialized) {
            parallelRedstone = new ParallelRedstone(1048576, true); // 1M 方块，直接内存
            initialized = true;
        }
    }
    
    /**
     * 拦截红石更新
     */
    @Inject(method = "tick", at = @At("HEAD"))
    public void tickRedstoneUltra(CallbackInfo ci) {
        initRedstone();
        
        // 使用并行红石系统
        parallelRedstone.parallelUpdate();
        parallelRedstone.processQueue();
    }
    
    /**
     * 拦截方块更新
     */
    @Inject(method = "blockUpdated", at = @At("HEAD"))
    public void blockUpdatedUltra(BlockPos pos, net.minecraft.world.level.block.Block block, CallbackInfo ci) {
        if (parallelRedstone != null && block.hasAnalogOutputSignal()) {
            // 重定向到并行红石系统
            // parallelRedstone.setSignal(...);
        }
    }
}
