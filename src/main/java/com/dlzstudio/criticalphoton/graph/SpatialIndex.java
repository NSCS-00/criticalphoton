package com.dlzstudio.criticalphoton.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 空间加速索引
 */
public class SpatialIndex {
    
    private final it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<Entity>> chunkIndex;
    private final Map<Entity, ChunkPos> entityChunkCache;
    
    public SpatialIndex() {
        this.chunkIndex = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        this.entityChunkCache = new ConcurrentHashMap<>();
    }
    
    public void addEntity(Entity entity) {
        updateEntityPosition(entity);
    }
    
    public void updateEntityPosition(Entity entity) {
        BlockPos pos = entity.blockPosition();
        ChunkPos chunkPos = new ChunkPos(pos);
        long chunkKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        
        ChunkPos oldChunkPos = entityChunkCache.get(entity);
        if (oldChunkPos != null && !oldChunkPos.equals(chunkPos)) {
            long oldKey = ChunkPos.asLong(oldChunkPos.x, oldChunkPos.z);
            List<Entity> oldChunkEntities = chunkIndex.get(oldKey);
            if (oldChunkEntities != null) {
                oldChunkEntities.remove(entity);
                if (oldChunkEntities.isEmpty()) chunkIndex.remove(oldKey);
            }
        }
        
        chunkIndex.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entity);
        entityChunkCache.put(entity, chunkPos);
    }
    
    public void removeEntity(Entity entity) {
        ChunkPos chunkPos = entityChunkCache.remove(entity);
        if (chunkPos != null) {
            long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);
            List<Entity> entities = chunkIndex.get(key);
            if (entities != null) {
                entities.remove(entity);
                if (entities.isEmpty()) chunkIndex.remove(key);
            }
        }
    }
    
    public List<Entity> getEntitiesInRange(double x, double y, double z, double radius) {
        List<Entity> result = new ArrayList<>();
        int minChunkX = (int) Math.floor((x - radius) / 16.0);
        int maxChunkX = (int) Math.ceil((x + radius) / 16.0);
        int minChunkZ = (int) Math.floor((z - radius) / 16.0);
        int maxChunkZ = (int) Math.ceil((z + radius) / 16.0);
        double radiusSquared = radius * radius;
        
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                List<Entity> chunkEntities = chunkIndex.get(key);
                if (chunkEntities != null) {
                    for (Entity entity : chunkEntities) {
                        double dx = entity.getX() - x;
                        double dy = entity.getY() - y;
                        double dz = entity.getZ() - z;
                        if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(entity);
                    }
                }
            }
        }
        return result;
    }
    
    public List<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        List<Entity> entities = chunkIndex.get(ChunkPos.asLong(chunkX, chunkZ));
        return entities != null ? entities : new ArrayList<>();
    }
    
    public void cleanup() {
        chunkIndex.values().removeIf(list -> { list.removeIf(Entity::isRemoved); return list.isEmpty(); });
        entityChunkCache.keySet().removeIf(Entity::isRemoved);
    }
    
    public int getTotalEntities() { return entityChunkCache.size(); }
}
