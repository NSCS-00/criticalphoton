package com.dlzstudio.criticalphoton.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CriticalPhotonConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        {
            final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
            CLIENT_SPEC = specPair.getRight();
            CLIENT = specPair.getLeft();
        }
        {
            final Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
            COMMON_SPEC = specPair.getRight();
            COMMON = specPair.getLeft();
        }
    }

    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue enableOptimizations;
        public final ModConfigSpec.BooleanValue enableAsyncRender;
        public final ModConfigSpec.IntValue maxRenderThreads;
        public final ModConfigSpec.IntValue targetFps;
        public final ModConfigSpec.IntValue minFps;
        public final ModConfigSpec.IntValue maxDependencyDistance;
        public final ModConfigSpec.BooleanValue enableVisualAnchors;
        public final ModConfigSpec.BooleanValue enableSmoothTransitions;
        public final ModConfigSpec.BooleanValue enablePerformanceOverlay;

        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("Rendering");
            enableOptimizations = builder.comment("启用优化").define("enableOptimizations", true);
            enableAsyncRender = builder.comment("启用异步渲染").define("enableAsyncRender", true);
            maxRenderThreads = builder.comment("最大渲染线程数").defineInRange("maxRenderThreads", 4, 1, 16);
            targetFps = builder.comment("目标 FPS").defineInRange("targetFps", 144, 30, 1000);
            minFps = builder.comment("最小可接受 FPS").defineInRange("minFps", 60, 30, 120);
            builder.pop();

            builder.push("VisualDependency");
            maxDependencyDistance = builder.comment("最大视觉依赖距离").defineInRange("maxDependencyDistance", 256, 64, 512);
            enableVisualAnchors = builder.comment("启用视觉锚点").define("enableVisualAnchors", true);
            enableSmoothTransitions = builder.comment("启用平滑过渡").define("enableSmoothTransitions", true);
            builder.pop();

            builder.push("PerformanceOverlay");
            enablePerformanceOverlay = builder.comment("启用性能监控覆盖层").define("enablePerformanceOverlay", false);
            builder.pop();
        }
    }

    public static class CommonConfig {
        public final ModConfigSpec.BooleanValue debugLogging;
        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.push("General");
            debugLogging = builder.comment("启用调试日志").define("debugLogging", false);
            builder.pop();
        }
    }
}
