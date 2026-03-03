# 临界光子 (Critical Photon)

Minecraft Java 版 1.21.1 NeoForge 极致性能优化客户端模组

## 模组信息

| 配置项 | 值 |
|--------|-----|
| **Mod ID** | `criticalphoton` |
| **Group ID** | `com.dlzstudio.criticalphoton` |
| **版本** | `0.3.0` |
| **Minecraft** | 1.21.1 |
| **NeoForge** | 21.1.166+ |

## 0.3.0 更新内容 - 极致性能版

### ⚡ 核心架构重构

#### UltraECS - 零对象开销实体系统
- 使用 `Unsafe` 直接内存操作
- 位图活跃实体追踪，O(1) 查找
- 手动循环展开，SIMD 友好
- **目标**: 10000 实体下 CPU 占用 < 20%

#### ParallelRedstone - 并行红石计算
- 位压缩信号存储 (4 位/方块)
- 多线程并行更新
- 延迟传播队列
- **目标**: 5000 高频红石下 CPU 占用 < 10%

#### ZeroOverheadRenderer - 零开销渲染器
- 直接内存顶点缓冲
- 批处理渲染 (减少状态切换)
- LOD 顶点数动态调整
- **目标**: 渲染调用减少 80%

#### UltraAsyncTickSystem - 完全异步 Tick
- 主线程 Tick 时间 < 5ms
- 工作线程并行实体 Tick
- 异步 IO 和计算任务
- **目标**: 20 TPS 稳定运行

#### ZeroGCAllocator - 零 GC 分配器
- 对象池重用
- 直接内存分配
- 无 GC 压力
- **目标**: GC 频率降低 95%

#### VectorMath - SIMD 向量运算
- 手动循环展开 (4x)
- 快速三角函数查表
- 快速倒数平方根 (Quake III 算法)
- **目标**: 向量运算性能提升 400%

### Mixin 注入优化

| Mixin | 功能 |
|-------|------|
| UltraECSMixin | 实体管理重定向到 ECS |
| ParallelRedstoneMixin | 红石更新重定向到并行系统 |
| UltraTickMixin | Tick 处理重定向到异步系统 |

### 性能对比 (4G 内存 + 集显 + i5)

| 场景 | 原版 | 0.2.x | 0.3.0 目标 |
|------|------|-------|-----------|
| 10000 实体 | 3-8 FPS | 8-15 FPS | **100-120 FPS** |
| 5000 红石 | 2-5 FPS | 6-12 FPS | **100-120 FPS** |
| 混合负载 | 1-3 FPS | 5-8 FPS | **80-100 FPS** |
| 内存占用 | 4GB+ | 3.5GB | **2.5GB** |
| GC 频率 | 频繁 | 中等 | **几乎无** |

## 0.2.x 更新内容

### 新增优化模块

#### 世界/区块优化
- **ChunkCacheOptimizer** - LRU 策略区块缓存，使用 StampedLock 无锁快速路径
- **BlockStateCacheOptimizer** - 扁平数组方块状态存储，SIMD 友好批量获取
- **LightDataOptimizer** - 位压缩光照数据，延迟更新队列

#### 实体优化
- **EntityIdAllocator** - 位图 ID 分配，O(1) 分配/释放
- **EntitySpatialPartition** - 均匀网格空间分区，加速实体查询
- **EntityComponentSystem** - ECS 风格数据导向设计，批量位置更新

#### 方块优化
- **BlockUpdateOptimizer** - 批量方块更新，去重位图避免冗余更新
- **TileEntityOptimizer** - 方块实体惰性更新，距离优先级排序

#### 粒子优化
- **ParticleOptimizer** - 对象池粒子管理，扁平数组存储，零拷贝访问

#### 网络优化
- **PacketOptimizer** - 直接内存数据包，可变长度编码
- **NetworkSyncOptimizer** - 增量同步，优先级队列

#### 内存优化
- **MemoryPoolOptimizer** - 直接内存池，对象池重用，减少 GC 压力

#### Tick 优化
- **TickScheduler** - 时间切片 Tick 处理，优先级调度
- **AsyncTickHandler** - 异步 Tick 处理器，后台非关键逻辑

### Mixin 优化注入
- LevelOptimizerMixin - 区块获取缓存
- EntityOptimizerMixin - 实体空间分区查询
- ParticleOptimizerMixin - 粒子批量更新
- TickOptimizerMixin - Tick 时间切片
- NetworkOptimizerMixin - 网络内存池

## 安装

### 前置要求

- Minecraft 1.21.1
- NeoForge 21.1.166+
- Java 21 (必须，0.3.0 使用 Unsafe API)

### 安装步骤

1. 安装 NeoForge 1.21.1
2. 将构建好的 jar 文件放入 `.minecraft/mods` 目录
3. 启动游戏

## 配置

### 快捷键

- **Ctrl + P** - 切换性能监控覆盖层
- **Ctrl + Shift + O** - 重置性能调节器

### 配置文件

配置文件位于 `config/DLZstudio/criticalphoton-client.toml`

## API 使用

### 为模组实体注册视觉锚点

```java
import com.dlzstudio.criticalphoton.api.CriticalPhotonApi;
import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

CriticalPhotonApi.registerVisualAnchor(entity, List.of(anchorPos), 
    new VisualAnchorConfig.Builder()
        .fadeInTime(0.3f)
        .fadeOutTime(0.5f)
        .minAlpha(0.1f)
        .build());
```

## 构建

```bash
gradlew build
```

## 注意事项

⚠️ **0.3.0 使用 `sun.misc.Unsafe`，这是内部 API**
- 需要 JVM 参数：`--add-opens java.base/sun.misc=ALL-UNNAMED`
- 在某些 JVM 上可能需要额外配置
- 生产环境使用请谨慎测试

## 许可证

MIT License

---

**临界光子 v0.3.0** - 性能压榨到极致，目标 100+ FPS
