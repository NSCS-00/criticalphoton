package com.dlzstudio.criticalphoton.renderer.core;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;

/**
 * 实体渲染批次
 * 
 * 将相同类型的实体分组渲染，减少状态切换
 * 
 * @author DLZstudio
 * @version 0.4.0
 */
public class RenderBatch {
    
    private Entity[] entities;
    private int size;
    private int capacity;
    
    public RenderBatch() {
        this(64);
    }
    
    public RenderBatch(int initialCapacity) {
        this.entities = new Entity[initialCapacity];
        this.size = 0;
        this.capacity = initialCapacity;
    }
    
    /**
     * 添加实体到批次
     */
    public void add(Entity entity) {
        if (size >= capacity) {
            // 扩容
            int newCapacity = capacity * 2;
            entities = Arrays.copyOf(entities, newCapacity);
            capacity = newCapacity;
        }
        entities[size++] = entity;
    }
    
    /**
     * 获取所有实体
     */
    public Entity[] getEntities() {
        return Arrays.copyOf(entities, size);
    }
    
    /**
     * 获取实体数量
     */
    public int size() {
        return size;
    }
    
    /**
     * 清空批次
     */
    public void clear() {
        size = 0;
        // 不释放数组，复用内存
        Arrays.fill(entities, null);
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }
}
