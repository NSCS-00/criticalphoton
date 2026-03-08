package com.dlzstudio.criticalphoton.renderer.core;

import com.dlzstudio.criticalphoton.CriticalPhoton;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * 临界光子渲染引擎核心类
 * 
 * 负责管理 OpenGL 状态、着色器、纹理等底层资源
 * 
 * @author DLZstudio
 * @version 0.4.0
 */
public class CriticalPhotonRenderer {
    
    private static CriticalPhotonRenderer instance;
    
    private final Minecraft mc;
    private long renderThread;
    
    // 统计信息
    private int drawCalls;
    private int verticesRendered;
    private int batchesRendered;
    
    public CriticalPhotonRenderer() {
        this.mc = Minecraft.getInstance();
        this.renderThread = Thread.currentThread().getId();
        CriticalPhoton.LOGGER.info("临界光子渲染引擎初始化 (0.4.0)");
    }
    
    public static CriticalPhotonRenderer get() {
        if (instance == null) {
            instance = new CriticalPhotonRenderer();
        }
        return instance;
    }
    
    /**
     * 在渲染线程上执行
     */
    public void executeOnRenderThread(Runnable runnable) {
        if (Thread.currentThread().getId() == renderThread) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }
    
    /**
     * 开始渲染批次
     */
    public void beginBatch() {
        drawCalls = 0;
        verticesRendered = 0;
        batchesRendered = 0;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }
    
    /**
     * 结束渲染批次
     */
    public void endBatch() {
        RenderSystem.disableBlend();
        
        CriticalPhoton.LOGGER.debug("渲染批次完成：{} 调用，{} 顶点，{} 批次", 
            drawCalls, verticesRendered, batchesRendered);
    }
    
    /**
     * 增加绘制调用计数
     */
    public void recordDrawCall(int vertices) {
        drawCalls++;
        verticesRendered += vertices;
    }
    
    /**
     * 增加批次计数
     */
    public void recordBatch() {
        batchesRendered++;
    }
    
    /**
     * 获取绘制调用次数
     */
    public int getDrawCalls() {
        return drawCalls;
    }
    
    /**
     * 获取渲染顶点数
     */
    public int getVerticesRendered() {
        return verticesRendered;
    }
    
    /**
     * 获取批次数
     */
    public int getBatchesRendered() {
        return batchesRendered;
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        drawCalls = 0;
        verticesRendered = 0;
        batchesRendered = 0;
    }
}
