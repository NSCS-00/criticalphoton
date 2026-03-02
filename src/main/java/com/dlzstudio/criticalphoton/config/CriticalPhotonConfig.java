package com.dlzstudio.criticalphoton.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 临界光子配置系统
 */
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
        
        public final ModConfigSpec.BooleanValue enableAsyncRender;
        public final ModConfigSpec.IntValue maxRenderThreads;
        public final ModConfigSpec.IntValue targetFps;
        public final ModConfigSpec.IntValue minFps;
        
        public final ModConfigSpec.IntValue maxDependencyDistance;
        public final ModConfigSpec.BooleanValue enableVisualAnchors;
        public final ModConfigSpec.BooleanValue enableSmoothTransitions;
        
        public final ModConfigSpec.DoubleValue lodScreenSpaceError;
        public final ModConfigSpec.IntValue lodMinSwitchFrames;
        public final ModConfigSpec.BooleanValue enableLodMorphing;
        
        public final ModConfigSpec.BooleanValue enableExtendedFrustum;
        public final ModConfigSpec.BooleanValue enableAlphaTransition;
        public final ModConfigSpec.DoubleValue fadeInTime;
        public final ModConfigSpec.DoubleValue fadeOutTime;
        
        public final ModConfigSpec.BooleanValue enablePerformanceOverlay;
        public final ModConfigSpec.IntValue overlayPositionX;
        public final ModConfigSpec.IntValue overlayPositionY;
        
        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("Rendering");
            
            enableAsyncRender = builder
                    .comment("启用异步渲染管线 (需要重启)")
                    .define("enableAsyncRender", true);
            
            maxRenderThreads = builder
                    .comment("最大渲染线程数")
                    .defineInRange("maxRenderThreads", 4, 1, 16);
            
            targetFps = builder
                    .comment("目标 FPS")
                    .defineInRange("targetFps", 144, 30, 1000);
            
            minFps = builder
                    .comment("最小可接受 FPS")
                    .defineInRange("minFps", 60, 30, 120);
            
            builder.pop();
            
            builder.push("VisualDependency");
            
            maxDependencyDistance = builder
                    .comment("最大视觉依赖距离 (方块单位)")
                    .defineInRange("maxDependencyDistance", 256, 64, 512);
            
            enableVisualAnchors = builder
                    .comment("启用视觉锚点系统")
                    .define("enableVisualAnchors", true);
            
            enableSmoothTransitions = builder
                    .comment("启用平滑过渡 (淡入淡出)")
                    .define("enableSmoothTransitions", true);
            
            builder.pop();
            
            builder.push("LOD");
            
            lodScreenSpaceError = builder
                    .comment("LOD 屏幕空间误差阈值 (像素)")
                    .defineInRange("lodScreenSpaceError", 4.0, 1.0, 16.0);
            
            lodMinSwitchFrames = builder
                    .comment("LOD 切换最小帧数间隔")
                    .defineInRange("lodMinSwitchFrames", 10, 1, 60);
            
            enableLodMorphing = builder
                    .comment("启用 LOD 几何变形过渡")
                    .define("enableLodMorphing", true);
            
            builder.pop();
            
            builder.push("Culling");
            
            enableExtendedFrustum = builder
                    .comment("启用扩展视锥体剔除")
                    .define("enableExtendedFrustum", true);
            
            enableAlphaTransition = builder
                    .comment("启用透明度渐变过渡")
                    .define("enableAlphaTransition", true);
            
            fadeInTime = builder
                    .comment("淡入时间 (秒)")
                    .defineInRange("fadeInTime", 0.3, 0.1, 2.0);
            
            fadeOutTime = builder
                    .comment("淡出时间 (秒)")
                    .defineInRange("fadeOutTime", 0.5, 0.1, 2.0);
            
            builder.pop();
            
            builder.push("PerformanceOverlay");
            
            enablePerformanceOverlay = builder
                    .comment("启用性能监控覆盖层")
                    .define("enablePerformanceOverlay", false);
            
            overlayPositionX = builder
                    .comment("覆盖层 X 位置")
                    .defineInRange("overlayPositionX", 10, 0, 10000);
            
            overlayPositionY = builder
                    .comment("覆盖层 Y 位置")
                    .defineInRange("overlayPositionY", 10, 0, 10000);
            
            builder.pop();
        }
    }
    
    public static class CommonConfig {
        
        public final ModConfigSpec.BooleanValue enableOptimizations;
        public final ModConfigSpec.BooleanValue debugLogging;
        
        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.push("General");
            
            enableOptimizations = builder
                    .comment("启用所有优化")
                    .define("enableOptimizations", true);
            
            debugLogging = builder
                    .comment("启用调试日志")
                    .define("debugLogging", false);
            
            builder.pop();
        }
    }
}
