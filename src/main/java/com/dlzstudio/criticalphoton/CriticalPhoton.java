package com.dlzstudio.criticalphoton;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CriticalPhoton.MOD_ID)
public class CriticalPhoton {
    public static final String MOD_ID = "criticalphoton";
    public static final String MOD_NAME = "临界光子";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public CriticalPhoton(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CriticalPhotonConfig.CLIENT_SPEC, "DLZstudio/criticalphoton-client");
        container.registerConfig(ModConfig.Type.COMMON, CriticalPhotonConfig.COMMON_SPEC, "DLZstudio/criticalphoton-common");

        modEventBus.addListener(this::clientSetup);

        LOGGER.info("临界光子 v0.3.0 已加载 - 性能优化引擎启动");
        LOGGER.info("配置文件路径：./config/DLZstudio/criticalphoton-client.toml");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("客户端设置完成");
        });
    }
}
