package com.dlzstudio.criticalphoton;

import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.renderer.AsyncRenderPipeline;
import com.dlzstudio.criticalphoton.renderer.core.CriticalPhotonRenderer;
import com.dlzstudio.criticalphoton.renderer.core.RenderBatchManager;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import com.dlzstudio.criticalphoton.system.VisualDependencyManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CriticalPhoton.MOD_ID)
public class CriticalPhoton {
    public static final String MOD_ID = "criticalphoton";
    public static final String MOD_NAME = "临界光子";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static CriticalPhoton instance;
    private final VisualDependencyManager dependencyManager;
    private final AsyncRenderPipeline renderPipeline;
    private final PerformanceMonitor performanceMonitor;
    
    // 0.4.0 新渲染引擎
    private CriticalPhotonRenderer renderer;
    private RenderBatchManager batchManager;

    public CriticalPhoton(IEventBus modEventBus, ModContainer container) {
        instance = this;

        container.registerConfig(ModConfig.Type.CLIENT, CriticalPhotonConfig.CLIENT_SPEC, "DLZstudio/criticalphoton-client");
        container.registerConfig(ModConfig.Type.COMMON, CriticalPhotonConfig.COMMON_SPEC, "DLZstudio/criticalphoton-common");

        dependencyManager = new VisualDependencyManager();
        renderPipeline = new AsyncRenderPipeline();
        performanceMonitor = new PerformanceMonitor();
        
        // 初始化 0.4.0 渲染引擎
        renderer = CriticalPhotonRenderer.get();
        batchManager = RenderBatchManager.get();

        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);

        LOGGER.info("临界光子 v0.4.0 已加载 - 全新渲染引擎");
        LOGGER.info("配置文件路径：./config/DLZstudio/criticalphoton-client.toml");
    }

    public static CriticalPhoton getInstance() { return instance; }
    public VisualDependencyManager getDependencyManager() { return dependencyManager; }
    public AsyncRenderPipeline getRenderPipeline() { return renderPipeline; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public CriticalPhotonRenderer getRenderer() { return renderer; }
    public RenderBatchManager getBatchManager() { return batchManager; }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("客户端设置完成，渲染管线初始化");
            renderPipeline.initialize();
            performanceMonitor.start();
        });
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        performanceMonitor.update();
        dependencyManager.update();
        renderPipeline.update();
        
        // 每帧开始新的批次收集
        batchManager.beginFrame();
    }
    
    private void onRenderLevel(final RenderLevelStageEvent event) {
        // 使用新渲染引擎接管渲染
        if (renderer != null) {
            renderer.beginBatch();
        }
    }
}
