package com.dlzstudio.criticalphoton.api;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 临界光子视觉锚点 API
 * 
 * 为模组开发者提供注册实体视觉依赖的接口
 */
public class CriticalPhotonApi {
    
    public static void registerVisualAnchor(Entity entity, List<BlockPos> anchorBlocks, VisualAnchorConfig config) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            instance.getDependencyManager().registerVisualAnchor(entity, anchorBlocks, config);
        }
    }
    
    public static void registerVisualAnchor(Entity entity, BlockPos anchorBlock) {
        registerVisualAnchor(entity, List.of(anchorBlock), new VisualAnchorConfig.Builder().build());
    }
    
    public static void registerVisualAnchor(Entity entity, BlockPos anchorBlock, 
                                            float fadeInTime, float fadeOutTime, float minAlpha) {
        VisualAnchorConfig config = new VisualAnchorConfig.Builder()
                .fadeInTime(fadeInTime)
                .fadeOutTime(fadeOutTime)
                .minAlpha(minAlpha)
                .build();
        registerVisualAnchor(entity, List.of(anchorBlock), config);
    }
    
    public static void unregisterVisualAnchor(Entity entity) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            instance.getDependencyManager().removeEntity(entity);
        }
    }
    
    public static boolean hasVisualAnchor(Entity entity) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return false;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            return instance.getDependencyManager().getDependencyGraph()
                    .getDependentBlocks(entity) != null;
        }
        return false;
    }
    
    public static float getRenderAlpha(Entity entity, float partialTick) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return 1.0f;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            return instance.getDependencyManager().getRenderAlpha(entity, partialTick);
        }
        return 1.0f;
    }
    
    public static int getLodLevel(Entity entity) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return 0;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            return instance.getDependencyManager().getLodLevel(entity);
        }
        return 0;
    }
    
    public static boolean shouldRender(Entity entity) {
        if (!CriticalPhotonConfigHelper.isModLoaded()) {
            return true;
        }
        
        var instance = CriticalPhoton.getInstance();
        if (instance != null) {
            return instance.getDependencyManager().shouldRender(entity);
        }
        return true;
    }
    
    public static VisualAnchorConfig.Builder createAnchorConfigBuilder() {
        return new VisualAnchorConfig.Builder();
    }
}
