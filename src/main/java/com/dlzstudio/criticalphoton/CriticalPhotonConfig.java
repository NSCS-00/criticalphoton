package com.dlzstudio.criticalphoton;

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
        public final ModConfigSpec.IntValue maxRenderThreads;
        public final ModConfigSpec.IntValue targetFps;

        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("General");
            enableOptimizations = builder.comment("启用优化").define("enableOptimizations", true);
            maxRenderThreads = builder.comment("最大渲染线程数").defineInRange("maxRenderThreads", 4, 1, 16);
            targetFps = builder.comment("目标 FPS").defineInRange("targetFps", 144, 30, 1000);
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
