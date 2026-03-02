package com.dlzstudio.criticalphoton.renderer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * 扩展视锥体渲染器
 */
public class ExtendedFrustumRenderer {
    
    private final net.minecraft.client.renderer.culling.Frustum primaryFrustum;
    private ExtendedFrustum extendedFrustum;
    private final java.util.Set<Long> potentiallyVisibleChunks;
    private final int maxDependencyDistance;
    
    public ExtendedFrustumRenderer(int maxDependencyDistance) {
        this.primaryFrustum = new net.minecraft.client.renderer.culling.Frustum();
        this.potentiallyVisibleChunks = new java.util.concurrent.ConcurrentHashMap().newKeySet();
        this.maxDependencyDistance = maxDependencyDistance;
    }
    
    public void update(net.minecraft.client.Camera camera, double x, double y, double z) {
        primaryFrustum.prepare(x, y, z);
        this.extendedFrustum = calculateExtendedFrustum(primaryFrustum, x, y, z);
        updatePotentiallyVisibleChunks(x, y, z);
    }
    
    private ExtendedFrustum calculateExtendedFrustum(
            net.minecraft.client.renderer.culling.Frustum baseFrustum,
            double x, double y, double z) {
        float extendDistance = maxDependencyDistance;
        return new ExtendedFrustum(
            x - extendDistance, y - extendDistance, z - extendDistance,
            x + extendDistance, y + extendDistance, z + extendDistance);
    }
    
    private void updatePotentiallyVisibleChunks(double x, double y, double z) {
        potentiallyVisibleChunks.clear();
        int chunkRange = (int) Math.ceil(maxDependencyDistance / 16.0);
        int playerChunkX = (int) Math.floor(x / 16.0);
        int playerChunkZ = (int) Math.floor(z / 16.0);
        
        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                double chunkCenterX = (chunkX * 16 + 8);
                double chunkCenterZ = (chunkZ * 16 + 8);
                if (extendedFrustum != null && extendedFrustum.contains(chunkCenterX, y, chunkCenterZ)) {
                    potentiallyVisibleChunks.add(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
                }
            }
        }
    }
    
    public boolean isVisibleInPrimary(Entity entity) {
        return primaryFrustum.isVisible(entity.getBoundingBox());
    }
    
    public boolean isVisibleInExtended(double x, double y, double z) {
        return extendedFrustum != null && extendedFrustum.contains(x, y, z);
    }
    
    public boolean isChunkPotentiallyVisible(int chunkX, int chunkZ) {
        return potentiallyVisibleChunks.contains(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
    }
    
    public java.util.Set<Long> getPotentiallyVisibleChunks() { return potentiallyVisibleChunks; }
    public net.minecraft.client.renderer.culling.Frustum getPrimaryFrustum() { return primaryFrustum; }
    public ExtendedFrustum getExtendedFrustum() { return extendedFrustum; }
    
    public static class ExtendedFrustum {
        private final double minX, minY, minZ, maxX, maxY, maxZ;
        
        public ExtendedFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }
        
        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        
        public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            return this.minX <= maxX && this.maxX >= minX &&
                   this.minY <= maxY && this.maxY >= minY &&
                   this.minZ <= maxZ && this.maxZ >= minZ;
        }
    }
}
