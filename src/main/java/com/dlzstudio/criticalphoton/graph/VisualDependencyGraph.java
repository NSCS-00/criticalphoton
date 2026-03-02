package com.dlzstudio.criticalphoton.graph;

import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视觉依赖图 - 核心数据结构
 */
public class VisualDependencyGraph {
    
    private final Map<Long, List<Entity>> blockToEntities;
    private final Map<Entity, Set<Long>> entityToBlocks;
    private final Map<Entity, VisualAnchorConfig> entityAnchorConfigs;
    private final SpatialIndex spatialIndex;
    private final Set<Entity> potentiallyVisibleEntities;
    private final int maxDependencyDistance;
    
    public VisualDependencyGraph() {
        this.blockToEntities = new ConcurrentHashMap<>();
        this.entityToBlocks = new ConcurrentHashMap<>();
        this.entityAnchorConfigs = new ConcurrentHashMap<>();
        this.spatialIndex = new SpatialIndex();
        this.potentiallyVisibleEntities = ConcurrentHashMap.newKeySet();
        this.maxDependencyDistance = CriticalPhotonConfig.CLIENT.maxDependencyDistance.get();
    }
    
    public void registerDependency(Entity entity, List<BlockPos> anchorBlocks, VisualAnchorConfig config) {
        removeEntity(entity);
        
        Set<Long> blockKeys = new HashSet<>();
        for (BlockPos pos : anchorBlocks) {
            long key = pos.asLong();
            blockKeys.add(key);
            blockToEntities.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(entity);
        }
        
        entityToBlocks.put(entity, blockKeys);
        entityAnchorConfigs.put(entity, config);
        spatialIndex.addEntity(entity);
    }
    
    public void removeEntity(Entity entity) {
        Set<Long> blocks = entityToBlocks.remove(entity);
        if (blocks != null) {
            for (long blockKey : blocks) {
                List<Entity> entities = blockToEntities.get(blockKey);
                if (entities != null) {
                    entities.remove(entity);
                    if (entities.isEmpty()) {
                        blockToEntities.remove(blockKey);
                    }
                }
            }
        }
        entityAnchorConfigs.remove(entity);
        spatialIndex.removeEntity(entity);
        potentiallyVisibleEntities.remove(entity);
    }
    
    public void updateChunkVisibility(Level level, LevelChunk chunk, FrustumVisibility frustum) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        
        for (long blockKey : blockToEntities.keySet()) {
            BlockPos pos = BlockPos.of(blockKey);
            if (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                if (isPotentiallyVisible(frustum, pos)) {
                    List<Entity> entities = blockToEntities.get(blockKey);
                    if (entities != null) {
                        for (Entity entity : entities) {
                            scheduleEntityRender(entity);
                        }
                    }
                }
            }
        }
    }
    
    public boolean isPotentiallyVisible(FrustumVisibility frustum, BlockPos pos) {
        if (frustum.isVisible(pos)) {
            return true;
        }
        return frustum.isInExtendedFrustum(pos, maxDependencyDistance);
    }
    
    public void scheduleEntityRender(Entity entity) {
        potentiallyVisibleEntities.add(entity);
    }
    
    public VisualAnchorConfig getAnchorConfig(Entity entity) {
        return entityAnchorConfigs.get(entity);
    }
    
    public List<Entity> getDependentEntities(BlockPos pos) {
        List<Entity> entities = blockToEntities.get(pos.asLong());
        return entities != null ? entities : Collections.emptyList();
    }
    
    public Set<BlockPos> getDependentBlocks(Entity entity) {
        Set<Long> keys = entityToBlocks.get(entity);
        if (keys == null) return Collections.emptySet();
        Set<BlockPos> positions = new HashSet<>(keys.size());
        for (long key : keys) positions.add(BlockPos.of(key));
        return positions;
    }
    
    public Set<Entity> getPotentiallyVisibleEntities() {
        return Collections.unmodifiableSet(potentiallyVisibleEntities);
    }
    
    public void clearVisibilityCache() {
        potentiallyVisibleEntities.clear();
    }
    
    public List<Entity> getEntitiesInRange(double x, double y, double z, double radius) {
        return spatialIndex.getEntitiesInRange(x, y, z, radius);
    }
    
    public void cleanup() {
        entityToBlocks.keySet().removeIf(Entity::isRemoved);
        entityAnchorConfigs.keySet().removeIf(Entity::isRemoved);
        blockToEntities.values().removeIf(list -> { list.removeIf(Entity::isRemoved); return list.isEmpty(); });
        spatialIndex.cleanup();
    }
}
