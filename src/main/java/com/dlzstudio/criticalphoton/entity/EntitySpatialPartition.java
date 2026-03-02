package com.dlzstudio.criticalphoton.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体空间分区优化器
 * 使用均匀网格加速实体查询
 */
public class EntitySpatialPartition {
    
    // 网格单元
    private final List<Entity>[][] grid;
    private final int gridSize;
    private final float cellSize;
    private final float minX, minY, minZ;
    private final float maxX, maxY, maxZ;
    
    // 实体到网格位置的映射
    private final java.util.Map<Entity, GridPosition> entityGridMap;
    
    public EntitySpatialPartition(float minX, float minY, float minZ, 
                                   float maxX, float maxY, float maxZ, 
                                   float cellSize) {
        this.cellSize = cellSize;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        
        this.gridSize = (int) Math.ceil((maxX - minX) / cellSize);
        this.grid = createGrid(gridSize);
        this.entityGridMap = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    @SuppressWarnings("unchecked")
    private List<Entity>[][] createGrid(int size) {
        List<Entity>[][] grid = new List[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                grid[x][z] = new ArrayList<>(4);
            }
        }
        return grid;
    }
    
    /**
     * 添加实体
     */
    public void addEntity(Entity entity) {
        GridPosition pos = getGridPosition(entity);
        if (pos != null && isValidGridPos(pos)) {
            grid[pos.x][pos.z].add(entity);
            entityGridMap.put(entity, pos);
        }
    }
    
    /**
     * 移除实体
     */
    public void removeEntity(Entity entity) {
        GridPosition pos = entityGridMap.remove(entity);
        if (pos != null && isValidGridPos(pos)) {
            grid[pos.x][pos.z].remove(entity);
        }
    }
    
    /**
     * 更新实体位置
     */
    public void updateEntityPosition(Entity entity) {
        GridPosition oldPos = entityGridMap.get(entity);
        GridPosition newPos = getGridPosition(entity);
        
        if (oldPos != null && newPos != null) {
            if (!oldPos.equals(newPos) && isValidGridPos(newPos)) {
                // 从旧网格移除
                if (isValidGridPos(oldPos)) {
                    grid[oldPos.x][oldPos.z].remove(entity);
                }
                // 添加到新网格
                grid[newPos.x][newPos.z].add(entity);
                entityGridMap.put(entity, newPos);
            }
        } else if (newPos != null && isValidGridPos(newPos)) {
            addEntity(entity);
        }
    }
    
    /**
     * 获取网格位置
     */
    private GridPosition getGridPosition(Entity entity) {
        float x = (float) entity.getX();
        float z = (float) entity.getZ();
        
        int gridX = (int) ((x - minX) / cellSize);
        int gridZ = (int) ((z - minZ) / cellSize);
        
        return new GridPosition(gridX, gridZ);
    }
    
    /**
     * 查询范围内的实体
     */
    public List<Entity> getEntitiesInRange(double x, double y, double z, double radius) {
        List<Entity> result = new ArrayList<>();
        
        int minGridX = (int) ((x - radius - minX) / cellSize);
        int maxGridX = (int) ((x + radius - minX) / cellSize);
        int minGridZ = (int) ((z - radius - minZ) / cellSize);
        int maxGridZ = (int) ((z + radius - minZ) / cellSize);
        
        double radiusSquared = radius * radius;
        
        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                if (isValidGridIndex(gx, gz)) {
                    for (Entity entity : grid[gx][gz]) {
                        double dx = entity.getX() - x;
                        double dz = entity.getZ() - z;
                        if (dx * dx + dz * dz <= radiusSquared) {
                            result.add(entity);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 查询包围盒内的实体
     */
    public List<Entity> getEntitiesInAABB(AABB bounds) {
        List<Entity> result = new ArrayList<>();
        
        int minGridX = (int) ((bounds.minX - minX) / cellSize);
        int maxGridX = (int) ((bounds.maxX - minX) / cellSize);
        int minGridZ = (int) ((bounds.minZ - minZ) / cellSize);
        int maxGridZ = (int) ((bounds.maxZ - minZ) / cellSize);
        
        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                if (isValidGridIndex(gx, gz)) {
                    for (Entity entity : grid[gx][gz]) {
                        if (entity.getBoundingBox().intersects(bounds)) {
                            result.add(entity);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    private boolean isValidGridPos(GridPosition pos) {
        return isValidGridIndex(pos.x, pos.z);
    }
    
    private boolean isValidGridIndex(int x, int z) {
        return x >= 0 && x < gridSize && z >= 0 && z < gridSize;
    }
    
    /**
     * 清空分区
     */
    public void clear() {
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                grid[x][z].clear();
            }
        }
        entityGridMap.clear();
    }
    
    /**
     * 获取实体总数
     */
    public int getEntityCount() {
        return entityGridMap.size();
    }
    
    private static class GridPosition {
        final int x, z;
        
        GridPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GridPosition)) return false;
            GridPosition that = (GridPosition) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }
}
