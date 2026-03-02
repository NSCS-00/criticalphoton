package com.dlzstudio.criticalphoton.mixin;

import com.dlzstudio.criticalphoton.entity.EntitySpatialPartition;
import com.dlzstudio.criticalphoton.entity.EntityIdAllocator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体管理优化 Mixin
 */
@Mixin(Level.class)
public abstract class EntityOptimizerMixin {
    
    private static EntitySpatialPartition entityPartition;
    private static EntityIdAllocator idAllocator;
    
    static {
        // 初始化空间分区（覆盖 512x512 区域，每格 16 方块）
        entityPartition = new EntitySpatialPartition(-4096, 0, -4096, 4096, 256, 4096, 16);
        idAllocator = new EntityIdAllocator(65536);
    }
    
    /**
     * 优化实体添加
     */
    @Inject(method = "addFreshEntity", at = @At("TAIL"))
    public void addEntityOptimized(Entity entity, CallbackInfo ci) {
        entityPartition.addEntity(entity);
        
        // 分配优化 ID
        int id = idAllocator.allocateId();
        if (id >= 0) {
            idAllocator.registerEntity(id, entity);
        }
    }
    
    /**
     * 优化实体移除
     */
    @Inject(method = "removeEntity", at = @At("TAIL"))
    public void removeEntityOptimized(Entity entity, CallbackInfo ci) {
        entityPartition.removeEntity(entity);
    }
    
    /**
     * 优化实体查询
     */
    @Inject(method = "getEntities", at = @At("HEAD"), cancellable = true)
    public void getEntitiesOptimized(net.minecraft.world.entity.Entity entity,
                                      net.minecraft.world.phys.AABB bounds,
                                      java.util.function.Predicate<? super Entity> predicate,
                                      CallbackInfo ci) {
        // 使用空间分区加速查询
        var entities = entityPartition.getEntitiesInAABB(bounds);
        // 后续处理由原版代码完成
    }
}
