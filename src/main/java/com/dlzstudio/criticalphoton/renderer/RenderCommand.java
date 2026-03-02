package com.dlzstudio.criticalphoton.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.Entity;

public class RenderCommand implements Comparable<RenderCommand> {
    
    private final Entity entity;
    private final int priority;
    private final int shaderHash;
    private final double distanceSqr;
    public long submitTime;
    private boolean sorted;
    
    public RenderCommand(Entity entity, int priority, int shaderHash, double distanceSqr) {
        this.entity = entity;
        this.priority = priority;
        this.shaderHash = shaderHash;
        this.distanceSqr = distanceSqr;
        this.sorted = false;
    }
    
    public void execute() {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("RenderCommand must be executed on render thread");
        }
        if (entity == null || entity.isRemoved()) return;
        
        var mc = net.minecraft.client.Minecraft.getInstance();
        var dispatcher = mc.getEntityRenderDispatcher();
        
        try {
            com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
            net.minecraft.client.renderer.MultiBufferSource bufferSource = dispatcher.renderBuffers().bufferSource();
            
            double x = entity.xOld + (entity.getX() - entity.xOld) * mc.getFrameTime();
            double y = entity.yOld + (entity.getY() - entity.yOld) * mc.getFrameTime();
            double z = entity.zOld + (entity.getZ() - entity.zOld) * mc.getFrameTime();
            
            poseStack.translate(x, y, z);
            dispatcher.render(entity, poseStack, bufferSource, 0xF000F0);
            bufferSource.endBatch();
        } catch (Exception e) {
            // 忽略异常
        }
    }
    
    public void markSorted() { this.sorted = true; }
    
    @Override
    public int compareTo(RenderCommand other) {
        if (this.priority != other.priority) return Integer.compare(this.priority, other.priority);
        if (this.shaderHash != other.shaderHash) return Integer.compare(this.shaderHash, other.shaderHash);
        return Double.compare(this.distanceSqr, other.distanceSqr);
    }
    
    public Entity getEntity() { return entity; }
    public int getPriority() { return priority; }
    public boolean isSorted() { return sorted; }
}
