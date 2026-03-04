package com.dlzstudio.criticalphoton;

import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.renderer.AsyncRenderPipeline;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import com.dlzstudio.criticalphoton.system.VisualDependencyManager;
import com.dlzstudio.criticalphoton.tick.AsyncTickHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
    private final AsyncTickHandler asyncTickHandler;

    public CriticalPhoton(IEventBus modEventBus, DistExecutor executor, ModContainer container) {
        instance = this;

        // 注册配置文件到自定义路径 ./config/DLZstudio/criticalphoton-client.toml
        container.registerConfig(ModConfig.Type.CLIENT, CriticalPhotonConfig.CLIENT_SPEC, "DLZstudio/criticalphoton-client");
        container.registerConfig(ModConfig.Type.COMMON, CriticalPhotonConfig.COMMON_SPEC, "DLZstudio/criticalphoton-common");

        dependencyManager = new VisualDependencyManager();
        renderPipeline = new AsyncRenderPipeline();
        performanceMonitor = new PerformanceMonitor();
        asyncTickHandler = new AsyncTickHandler(50, 1024);

        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);

        LOGGER.info("临界光子 v0.2.2 已加载 - 性能优化引擎启动");
        LOGGER.info("配置文件路径：./config/DLZstudio/criticalphoton-client.toml");
    }

    public static CriticalPhoton getInstance() {
        return instance;
    }

    public VisualDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public AsyncRenderPipeline getRenderPipeline() {
        return renderPipeline;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public AsyncTickHandler getAsyncTickHandler() {
        return asyncTickHandler;
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("客户端设置完成，渲染管线初始化");
            renderPipeline.initialize();
            performanceMonitor.start();
            asyncTickHandler.start();
        });
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        performanceMonitor.update();
        dependencyManager.update();
        renderPipeline.update();
    }
}
