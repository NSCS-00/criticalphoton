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

import java.util.*;

/**
 * 视觉依赖管理器
 */
public class VisualDependencyManager {
    
    private final VisualDependencyGraph dependencyGraph;
    private final FrustumVisibility frustumVisibility;
    private final Map<Entity, EntityVisibilityState> visibilityStates;
    private double cameraX, cameraY, cameraZ;
    private final int maxDependencyDistance;
    
    public VisualDependencyManager() {
        this.dependencyGraph = new VisualDependencyGraph();
        this.frustumVisibility = new FrustumVisibility();
        this.visibilityStates = new HashMap<>();
        this.maxDependencyDistance = 256;
    }
    
    public void registerVisualAnchor(Entity entity, List<BlockPos> anchorBlocks, VisualAnchorConfig config) {
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
        var gameRenderer = mc.gameRenderer;
        var camera = gameRenderer.getMainCamera();
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        float fov = mc.options.fov().get().floatValue();
        float renderDistance = mc.options.renderDistance().get() * 16;
        AABB viewArea = new AABB(
            cameraX - renderDistance, cameraY - renderDistance, cameraZ - renderDistance,
            cameraX + renderDistance, cameraY + renderDistance, cameraZ + renderDistance);
        frustumVisibility.update(cameraX, cameraY, cameraZ, yaw, pitch, fov, viewArea);
    }
    
    private void updateVisibleChunks(Level level) {
        if (level == null) return;
        int playerChunkX = (int) Math.floor(cameraX / 16.0);
        int playerChunkZ = (int) Math.floor(cameraZ / 16.0);
        int renderDistance = Math.min(Minecraft.getInstance().options.renderDistance().get(), maxDependencyDistance / 16);
        
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                LevelChunk chunk = level.getChunk(playerChunkX + dx, playerChunkZ + dz);
                if (!chunk.isEmpty()) {
                    dependencyGraph.updateChunkVisibility(level, chunk, frustumVisibility);
                }
            }
        }
    }
    
    public boolean shouldRender(Entity entity) {
        if (!dependencyGraph.getPotentiallyVisibleEntities().contains(entity)) {
            Set<BlockPos> dependentBlocks = dependencyGraph.getDependentBlocks(entity);
            if (dependentBlocks.isEmpty()) {
                return frustumVisibility.intersects(entity);
            }
            for (BlockPos pos : dependentBlocks) {
                if (frustumVisibility.isInExtendedFrustum(pos, maxDependencyDistance)) return true;
            }
            return false;
        }
        return true;
    }
    
    public float getRenderAlpha(Entity entity, float partialTick) {
        EntityVisibilityState state = visibilityStates.get(entity);
        if (state == null) return 1.0f;
        return state.calculateAlpha(shouldRender(entity), partialTick);
    }
    
    public int getLodLevel(Entity entity) {
        EntityVisibilityState state = visibilityStates.get(entity);
        if (state == null) return 0;
        double distance = entity.distanceToSqr(cameraX, cameraY, cameraZ);
        return state.getLodLevel((float) Math.sqrt(distance));
    }
    
    public VisualDependencyGraph getDependencyGraph() { return dependencyGraph; }
    public EntityVisibilityState getVisibilityState(Entity entity) { return visibilityStates.get(entity); }
}
