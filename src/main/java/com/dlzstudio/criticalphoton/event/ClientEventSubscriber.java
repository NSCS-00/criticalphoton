package com.dlzstudio.criticalphoton.event;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.renderer.overlay.PerformanceOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class ClientEventSubscriber {
    private static final PerformanceOverlay overlay = new PerformanceOverlay();
    private static int ticksSinceInit = 0;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ticksSinceInit++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ticksSinceInit % 100 == 0) {
            CriticalPhoton.getInstance().getDependencyManager().getDependencyGraph().cleanup();
        }
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_P && hasControlDown()) {
            PerformanceOverlay.toggleOverlay();
        }
        if (event.getKey() == GLFW.GLFW_KEY_O && hasControlDown() && hasShiftDown()) {
            CriticalPhoton.getInstance().getPerformanceMonitor().getThrottler().reset();
            CriticalPhoton.LOGGER.info("性能调节器已重置");
        }
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (CriticalPhotonConfig.CLIENT.enablePerformanceOverlay.get()) {
            overlay.renderOverlay(event.getGuiGraphics());
        }
    }
    
    private static boolean hasControlDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
    
    private static boolean hasShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
