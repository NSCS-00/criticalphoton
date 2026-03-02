package com.dlzstudio.criticalphoton;

import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.renderer.AsyncRenderPipeline;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import com.dlzstudio.criticalphoton.system.VisualDependencyManager;
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

    public CriticalPhoton(IEventBus modEventBus, DistExecutor executor, ModContainer container) {
        instance = this;
        
        container.registerConfig(ModConfig.Type.CLIENT, CriticalPhotonConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.COMMON, CriticalPhotonConfig.COMMON_SPEC);
        
        dependencyManager = new VisualDependencyManager();
        renderPipeline = new AsyncRenderPipeline();
        performanceMonitor = new PerformanceMonitor();
        
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        
        LOGGER.info("临界光子已加载 - 目标：千帧体验，视觉无损");
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
    }
}
