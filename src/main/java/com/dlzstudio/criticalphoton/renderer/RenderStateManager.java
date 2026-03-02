package com.dlzstudio.criticalphoton.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class RenderStateManager {
    
    private final Map<EntityType<?>, RenderTypeCacheEntry> renderTypeCache;
    private RenderType currentRenderType;
    private int stateSwitchCount;
    private int batchCount;
    private int totalCommands;
    
    public RenderStateManager() {
        this.renderTypeCache = new HashMap<>();
        this.currentRenderType = null;
        this.stateSwitchCount = 0;
        this.batchCount = 0;
        this.totalCommands = 0;
    }
    
    public RenderType getRenderType(Entity entity) {
        EntityType<?> type = entity.getType();
        RenderTypeCacheEntry entry = renderTypeCache.get(type);
        if (entry == null) {
            entry = createRenderTypeForEntity(entity);
            renderTypeCache.put(type, entry);
        }
        return entry.renderType;
    }
    
    private RenderTypeCacheEntry createRenderTypeForEntity(Entity entity) {
        ResourceLocation texture = getEntityTexture(entity);
        boolean isTranslucent = isEntityTranslucent(entity);
        RenderType renderType = isTranslucent ? 
            RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
        return new RenderTypeCacheEntry(renderType, isTranslucent);
    }
    
    private ResourceLocation getEntityTexture(Entity entity) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var dispatcher = mc.getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(entity);
        try {
            @SuppressWarnings("unchecked")
            var entityRenderer = (net.minecraft.client.renderer.entity.EntityRenderer<Entity>) renderer;
            var textureLocation = entityRenderer.getTextureLocation(entity);
            return textureLocation != null ? textureLocation : ResourceLocation.parse("textures/entity/placeholder.png");
        } catch (Exception e) {
            return ResourceLocation.parse("textures/entity/placeholder.png");
        }
    }
    
    private boolean isEntityTranslucent(Entity entity) {
        return entity.getType() == EntityType.ENDERMAN ||
               entity.getType() == EntityType.GHAST ||
               entity.getType() == EntityType.SHULKER;
    }
    
    public boolean prepareRenderState(RenderType renderType) {
        totalCommands++;
        if (currentRenderType != renderType) {
            stateSwitchCount++;
            currentRenderType = renderType;
            batchCount++;
            return true;
        }
        return false;
    }
    
    public void resetStats() { stateSwitchCount = 0; batchCount = 0; totalCommands = 0; }
    public int getStateSwitchCount() { return stateSwitchCount; }
    public int getBatchCount() { return batchCount; }
    public int getTotalCommands() { return totalCommands; }
    public float getBatchEfficiency() {
        if (totalCommands == 0) return 1.0f;
        return 1.0f - ((float) batchCount / totalCommands);
    }
    
    private static class RenderTypeCacheEntry {
        final RenderType renderType;
        final boolean isTranslucent;
        RenderTypeCacheEntry(RenderType renderType, boolean isTranslucent) {
            this.renderType = renderType;
            this.isTranslucent = isTranslucent;
        }
    }
}
