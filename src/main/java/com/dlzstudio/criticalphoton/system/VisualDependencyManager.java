package com.dlzstudio.criticalphoton.system;

import com.dlzstudio.criticalphoton.graph.FrustumVisibility;
import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import com.dlzstudio.criticalphoton.graph.VisualDependencyGraph;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VisualDependencyManager {
    private final VisualDependencyGraph dependencyGraph;
    private final FrustumVisibility frustumVisibility;
    private final Map<Entity, EntityVisibilityState> visibilityStates;
    private double cameraX, cameraY, cameraZ;
    
    public VisualDependencyManager() {
        this.dependencyGraph = new VisualDependencyGraph();
        this.frustumVisibility = new FrustumVisibility();
        this.visibilityStates = new HashMap<>();
    }
    
    public void registerVisualAnchor(Entity entity, java.util.List<BlockPos> anchorBlocks, VisualAnchorConfig config) {
        dependencyGraph.registerDependency(entity, anchorBlocks, config);
        visibilityStates.put(entity, new EntityVisibilityState(config));
    }
    
    public void removeEntity(Entity entity) {
        dependencyGraph.removeEntity(entity);
        visibilityStates.remove(entity);
    }
    
    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.cameraEntity == null) return;
        
        cameraX = mc.cameraEntity.getX();
        cameraY = mc.cameraEntity.getY();
        cameraZ = mc.cameraEntity.getZ();
        
        updateFrustum(mc);
        dependencyGraph.cleanup();
        visibilityStates.keySet().removeIf(Entity::isRemoved);
        dependencyGraph.clearVisibilityCache();
        updateVisibleChunks(mc.level);
    }
    
    private void updateFrustum(Minecraft mc) {
        var camera = mc.gameRenderer.getMainCamera();
        float renderDistance = mc.options.renderDistance().get() * 16;
        AABB viewArea = new AABB(cameraX - renderDistance, cameraY - renderDistance, cameraZ - renderDistance,
                                  cameraX + renderDistance, cameraY + renderDistance, cameraZ + renderDistance);
        frustumVisibility.update(cameraX, cameraY, cameraZ, camera.getYRot(), camera.getXRot(), 
                                  mc.options.fov().get().floatValue(), viewArea);
    }
    
    private void updateVisibleChunks(Level level) {
        int playerChunkX = (int) Math.floor(cameraX / 16.0);
        int playerChunkZ = (int) Math.floor(cameraZ / 16.0);
        int renderDistance = Math.min(Minecraft.getInstance().options.renderDistance().get(), 16);
        
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                LevelChunk chunk = level.getChunk(playerChunkX + dx, playerChunkZ + dz);
                if (!chunk.isEmpty()) dependencyGraph.updateChunkVisibility(level, chunk, frustumVisibility);
            }
        }
    }
    
    /**
     * 检查实体是否应该渲染
     * 0.3.1 修复：没有注册视觉锚点的实体始终渲染
     */
    public boolean shouldRender(Entity entity) {
        // 检查是否有视觉依赖注册
        Set<BlockPos> dependentBlocks = dependencyGraph.getDependentBlocks(entity);
        if (dependentBlocks.isEmpty()) {
            // 没有注册视觉锚点，始终渲染 (使用原版渲染逻辑)
            return true;
        }
        
        // 有视觉依赖，检查可见性
        if (!dependencyGraph.getPotentiallyVisibleEntities().contains(entity)) {
            for (BlockPos pos : dependentBlocks) {
                if (frustumVisibility.isInExtendedFrustum(pos, 256)) return true;
            }
            return false;
        }
        return true;
    }

    public float getRenderAlpha(Entity entity, float partialTick) {
        EntityVisibilityState state = visibilityStates.get(entity);
        return state != null ? state.calculateAlpha(shouldRender(entity), partialTick) : 1.0f;
    }
    
    public int getLodLevel(Entity entity) {
        EntityVisibilityState state = visibilityStates.get(entity);
        if (state == null) return 0;
        return state.getLodLevel((float) Math.sqrt(entity.distanceToSqr(cameraX, cameraY, cameraZ)));
    }
    
    public VisualDependencyGraph getDependencyGraph() { return dependencyGraph; }
}
