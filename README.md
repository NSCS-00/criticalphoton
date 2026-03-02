# 临界光子 (Critical Photon)

Minecraft Java 版 1.21.1 NeoForge 极致性能优化客户端模组

## 模组信息

| 配置项 | 值 |
|--------|-----|
| **Mod ID** | `criticalphoton` |
| **Group ID** | `com.dlzstudio.criticalphoton` |
| **版本** | `0.1.0` |
| **Minecraft** | 1.21.1 |
| **NeoForge** | 21.1.166+ |

## 特性

### 🎯 核心优化

- **视觉无损的智能剔除系统** - 根治模组实体因方块剔除导致的"视觉割裂"问题
- **零开销的异步渲染架构** - 三阶段渲染流水线，多线程并行处理
- **连续 LOD 过渡系统** - 6 级细节层次，屏幕空间误差控制
- **实时性能监控与调节** - 毫秒级监控，动态负载平衡

### 🔧 技术亮点

1. **视觉依赖图模块** - 记录实体与锚点方块的依赖关系
2. **扩展视锥体剔除** - 双层级可见性判定
3. **平滑过渡渲染** - 透明度渐变 + 几何变形过渡
4. **异步渲染管线** - 收集→排序→执行三阶段
5. **性能节流系统** - 自动调节 LOD、实体更新频率、粒子密度

## 安装

### 前置要求

- Minecraft 1.21.1
- NeoForge 21.1.166+
- Java 21

### 安装步骤

1. 安装 NeoForge 1.21.1
2. 将构建好的 jar 文件放入 `.minecraft/mods` 目录
3. 启动游戏

## 配置

### 快捷键

- **Ctrl + P** - 切换性能监控覆盖层
- **Ctrl + Shift + O** - 重置性能调节器

### 配置文件

配置文件位于 `config/criticalphoton-client.toml`

## API 使用

### 为模组实体注册视觉锚点

```java
import com.dlzstudio.criticalphoton.api.CriticalPhotonApi;
import com.dlzstudio.criticalphoton.graph.VisualAnchorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

// 在实体创建时注册
BlockPos anchorPos = entity.blockPosition();
CriticalPhotonApi.registerVisualAnchor(entity, List.of(anchorPos), 
    new VisualAnchorConfig.Builder()
        .fadeInTime(0.3f)
        .fadeOutTime(0.5f)
        .minAlpha(0.1f)
        .build());

// 简化版
CriticalPhotonApi.registerVisualAnchor(entity, anchorPos);

// 移除注册
CriticalPhotonApi.unregisterVisualAnchor(entity);
```

### 查询 API

```java
// 检查实体是否应该渲染
boolean shouldRender = CriticalPhotonApi.shouldRender(entity);

// 获取渲染透明度
float alpha = CriticalPhotonApi.getRenderAlpha(entity, partialTick);

// 获取 LOD 级别
int lodLevel = CriticalPhotonApi.getLodLevel(entity);
```

## 构建

```bash
# 构建
gradlew build

# 运行客户端
gradlew runClient
```

## 许可证

MIT License

---

**临界光子** - 为千帧体验而生
