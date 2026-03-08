package com.dlzstudio.criticalphoton.renderer.core;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体渲染批次管理器
 * 
 * 将实体按类型分组，实现批量渲染
 * 
 * @author DLZstudio
 * @version 0.4.0
 */
public class RenderBatchManager {
    
    private static RenderBatchManager instance;
    
    // 按实体类型分组的批次
    private final Map<EntityType<?>, RenderBatch> batches;
    
    // 当前帧的实体总数
    private int totalEntities;
    
    public RenderBatchManager() {
        this.batches = new HashMap<>();
        CriticalPhoton.LOGGER.info("渲染批次管理器初始化");
    }
    
    public static RenderBatchManager get() {
        if (instance == null) {
            instance = new RenderBatchManager();
        }
        return instance;
    }
    
    /**
     * 开始新帧的批次收集
     */
    public void beginFrame() {
        // 清空所有批次
        for (RenderBatch batch : batches.values()) {
            batch.clear();
        }
        totalEntities = 0;
    }
    
    /**
     * 添加实体到批次
     */
    public void addEntity(Entity entity) {
        EntityType<?> type = entity.getType();
        
        RenderBatch batch = batches.computeIfAbsent(type, k -> new RenderBatch(32));
        batch.add(entity);
        totalEntities++;
    }
    
    /**
     * 获取所有批次
     */
    public Iterable<RenderBatch> getBatches() {
        return batches.values();
    }
    
    /**
     * 获取实体总数
     */
    public int getTotalEntities() {
        return totalEntities;
    }
    
    /**
     * 获取批次数量
     */
    public int getBatchCount() {
        int count = 0;
        for (RenderBatch batch : batches.values()) {
            if (!batch.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
