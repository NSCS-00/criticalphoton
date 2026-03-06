package com.dlzstudio.criticalphoton.renderer.overlay;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.dlzstudio.criticalphoton.config.CriticalPhotonConfig;
import com.dlzstudio.criticalphoton.system.PerformanceMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.text.DecimalFormat;

public class PerformanceOverlay {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private final Minecraft mc;
    private Font font;
    private int posX = 10, posY = 10;
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int WARNING_COLOR = 0xFFFFFF00;
    private static final int DANGER_COLOR = 0xFFFF0000;

    public PerformanceOverlay() {
        this.mc = Minecraft.getInstance();
        this.font = null;
    }

    private void ensureFont() {
        if (font == null && mc != null) {
            this.font = mc.font;
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!CriticalPhotonConfig.CLIENT.enablePerformanceOverlay.get()) return;
        ensureFont();
        if (font == null) return;
        renderOverlay(event.getGuiGraphics());
    }

    public void renderOverlay(GuiGraphics guiGraphics) {
        ensureFont();
        if (font == null) return;
        
        PerformanceMonitor monitor = CriticalPhoton.getInstance().getPerformanceMonitor();
        PerformanceMonitor.FrameMetrics metrics = monitor.getFrameMetrics();
        String[] lines = collectPerformanceData(monitor, metrics);
        
        int maxWidth = 0;
        for (String line : lines) {
            int width = font.width(line);
            if (width > maxWidth) maxWidth = width;
        }
        
        int padding = 4, lineHeight = font.lineHeight + 2;
        int bgWidth = maxWidth + padding * 2;
        int bgHeight = lines.length * lineHeight + padding * 2;
        
        guiGraphics.fill(posX, posY, posX + bgWidth, posY + bgHeight, BACKGROUND_COLOR);
        guiGraphics.drawString(font, "§l§6临界光子性能监控", posX + padding, posY + padding, 0xFFD700);
        
        int y = posY + padding + font.lineHeight + 4;
        for (int i = 0; i < lines.length; i++) {
            guiGraphics.drawString(font, lines[i], posX + padding, y, getLineColor(i, monitor));
            y += lineHeight;
        }
    }
    
    private String[] collectPerformanceData(PerformanceMonitor monitor, PerformanceMonitor.FrameMetrics metrics) {
        int fps = monitor.getFps();
        double avgFrameTime = monitor.getAverageFrameTime();
        long memoryUsed = monitor.getUsedMemoryMB();
        long memoryMax = monitor.getMaxMemoryMB();
        double memoryPercent = monitor.getMemoryUsage() * 100;
        double renderTime = metrics.getTotalRenderTimeMs();
        double entityTime = metrics.getPhaseTimeMs(PerformanceMonitor.Phase.ENTITY_RENDER);
        double blockTime = metrics.getPhaseTimeMs(PerformanceMonitor.Phase.BLOCK_RENDER);
        int throttleLevel = monitor.getThrottler().getThrottleLevel();
        int lodBias = monitor.getThrottler().getLodBias();
        
        return new String[] {
            "§fFPS: §a" + fps + " §7(目标：" + CriticalPhotonConfig.CLIENT.targetFps.get() + ")",
            "§f帧时间：§a" + DECIMAL_FORMAT.format(avgFrameTime) + "ms",
            "§f渲染耗时：§a" + DECIMAL_FORMAT.format(renderTime) + "ms",
            "§f  - 实体：§e" + DECIMAL_FORMAT.format(entityTime) + "ms",
            "§f  - 区块：§e" + DECIMAL_FORMAT.format(blockTime) + "ms",
            "§f内存：§a" + memoryUsed + "MB §7/ §a" + memoryMax + "MB §7(§e" + DECIMAL_FORMAT.format(memoryPercent) + "%§7)",
            "§f节流级别：§" + (throttleLevel == 0 ? "a" : throttleLevel <= 1 ? "e" : "c") + throttleLevel + " §7(LOD 偏置：" + lodBias + ")"
        };
    }
    
    private int getLineColor(int index, PerformanceMonitor monitor) {
        int fps = monitor.getFps();
        int minFps = CriticalPhotonConfig.CLIENT.minFps.get();
        if (fps < minFps / 2) return DANGER_COLOR;
        if (fps < minFps) return WARNING_COLOR;
        return TEXT_COLOR;
    }
    
    public void setPosition(int x, int y) { this.posX = x; this.posY = y; }
}
