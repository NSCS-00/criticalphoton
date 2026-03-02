package com.dlzstudio.criticalphoton.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 视锥体可见性检查器
 */
public class FrustumVisibility {
    
    private final float[][] frustumPlanes;
    private AABB extendedBounds;
    private double cameraX, cameraY, cameraZ;
    private AABB frustumAABB;
    
    public FrustumVisibility() {
        this.frustumPlanes = new float[6][4];
        this.frustumAABB = AABB.NULL;
    }
    
    public void update(double cameraX, double cameraY, double cameraZ, 
                       float yaw, float pitch, float fov, AABB viewArea) {
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.frustumAABB = viewArea;
        
        float extendDistance = 64.0f;
        this.extendedBounds = viewArea.inflate(extendDistance);
        calculateFrustumPlanes(cameraX, cameraY, cameraZ, yaw, pitch, fov);
    }
    
    private void calculateFrustumPlanes(double cx, double cy, double cz,
                                        float yaw, float pitch, float fov) {
        frustumPlanes[0] = new float[]{0, 0, 1, 0.1f};
        frustumPlanes[1] = new float[]{0, 0, -1, 512.0f};
    }
    
    public boolean isVisible(BlockPos pos) {
        return isVisible(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
    
    public boolean isVisible(double x, double y, double z) {
        if (!frustumAABB.contains(new Vec3(x, y, z))) return false;
        for (float[] plane : frustumPlanes) {
            float distance = plane[0] * (float)x + plane[1] * (float)y + plane[2] * (float)z + plane[3];
            if (distance < 0) return false;
        }
        return true;
    }
    
    public boolean isInExtendedFrustum(BlockPos pos, int maxDistance) {
        double dx = pos.getX() + 0.5 - cameraX;
        double dy = pos.getY() + 0.5 - cameraY;
        double dz = pos.getZ() + 0.5 - cameraZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > maxDistance) return false;
        return extendedBounds.contains(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }
    
    public boolean intersects(Entity entity) {
        AABB entityBounds = entity.getBoundingBox();
        if (!frustumAABB.intersects(entityBounds)) return false;
        return isVisible(entity.getX(), entity.getY(), entity.getZ());
    }
    
    public boolean intersects(AABB bounds) {
        return frustumAABB.intersects(bounds);
    }
    
    public AABB getExtendedBounds() { return extendedBounds; }
}
