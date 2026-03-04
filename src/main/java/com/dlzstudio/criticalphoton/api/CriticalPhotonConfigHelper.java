package com.dlzstudio.criticalphoton.api;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class CriticalPhotonConfigHelper {
    private static final String MOD_ID = "criticalphoton";
    
    public static boolean isModLoaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }
    
    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
