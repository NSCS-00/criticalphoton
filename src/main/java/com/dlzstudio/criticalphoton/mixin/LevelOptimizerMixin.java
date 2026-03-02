package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.world.ChunkCacheOptimizer;
import com.dlzstudio.criticalphoton.world.BlockStateCacheOptimizer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界优化 Mixin
 */
@Mixin(Level.class)
public abstract class LevelOptimizerMixin {
    
    private static ChunkCacheOptimizer chunkCache;
    private static BlockStateCacheOptimizer blockCache;
    
    static {
        chunkCache = new ChunkCacheOptimizer(512);
        blockCache = new BlockStateCacheOptimizer(4096);
    }
    
    /**
     * 优化区块获取
     */
    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    public void getChunkOptimized(int chunkX, int chunkZ, 
                                   net.minecraft.world.level.chunk.ChunkStatus status,
                                   boolean require, CallbackInfoReturnable<LevelChunk> cir) {
        long chunkPos = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        LevelChunk cached = chunkCache.getChunkFast(chunkPos);
        
        if (cached != null && !cached.isRemoved()) {
            cir.setReturnValue(cached);
        }
    }
    
    /**
     * 缓存加载的区块
     */
    @Inject(method = "getChunk", at = @At("RETURN"))
    public void cacheLoadedChunk(int chunkX, int chunkZ,
                                 net.minecraft.world.level.chunk.ChunkStatus status,
                                 boolean require, CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null) {
            long chunkPos = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
            chunkCache.putChunk(chunkPos, cir.getReturnValue());
        }
    }
}
