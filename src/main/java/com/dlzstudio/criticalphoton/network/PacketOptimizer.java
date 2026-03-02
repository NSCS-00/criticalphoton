package com.dlzstudio.criticalphoton.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 网络数据包优化器
 * 使用直接内存和零拷贝优化网络传输
 */
public class PacketOptimizer {
    
    // 直接内存缓冲区（避免 JVM 堆内存拷贝）
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    
    // 压缩缓冲区
    private final byte[] compressionBuffer;
    
    // 统计信息
    private long totalBytesSent;
    private long totalPacketsSent;
    private long compressionSavings;
    
    public PacketOptimizer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.compressionBuffer = new byte[bufferSize];
        this.totalBytesSent = 0;
        this.totalPacketsSent = 0;
        this.compressionSavings = 0;
    }
    
    /**
     * 写入整数（可变长度编码）
     */
    public void writeVarInt(int value) {
        while ((value & 0xFFFFFF80) != 0) {
            directBuffer.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        directBuffer.put((byte) value);
    }
    
    /**
     * 写入浮点数
     */
    public void writeFloat(float value) {
        directBuffer.putFloat(value);
    }
    
    /**
     * 写入双精度数
     */
    public void writeDouble(double value) {
        directBuffer.putDouble(value);
    }
    
    /**
     * 写入字符串（UTF-8，前缀长度）
     */
    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        directBuffer.put(bytes);
    }
    
    /**
     * 写入字节数组
     */
    public void writeBytes(byte[] bytes, int offset, int length) {
        directBuffer.put(bytes, offset, length);
    }
    
    /**
     * 重置缓冲区
     */
    public void reset() {
        directBuffer.clear();
    }
    
    /**
     * 获取当前数据大小
     */
    public int size() {
        return directBuffer.position();
    }
    
    /**
     * 获取缓冲区（用于发送）
     */
    public ByteBuffer getBuffer() {
        directBuffer.flip();
        return directBuffer;
    }
    
    /**
     * 记录发送统计
     */
    public void recordSend(int bytes) {
        totalBytesSent += bytes;
        totalPacketsSent++;
    }
    
    /**
     * 记录压缩节省
     */
    public void recordCompressionSavings(int originalSize, int compressedSize) {
        compressionSavings += (originalSize - compressedSize);
    }
    
    /**
     * 获取总发送字节数
     */
    public long getTotalBytesSent() {
        return totalBytesSent;
    }
    
    /**
     * 获取总发送包数
     */
    public long getTotalPacketsSent() {
        return totalPacketsSent;
    }
    
    /**
     * 获取压缩节省字节数
     */
    public long getCompressionSavings() {
        return compressionSavings;
    }
    
    /**
     * 获取平均包大小
     */
    public double getAveragePacketSize() {
        return totalPacketsSent > 0 ? (double) totalBytesSent / totalPacketsSent : 0;
    }
    
    /**
     * 获取压缩率
     */
    public double getCompressionRatio() {
        long totalOriginal = totalBytesSent + compressionSavings;
        return totalOriginal > 0 ? (double) compressionSavings / totalOriginal : 0;
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        totalBytesSent = 0;
        totalPacketsSent = 0;
        compressionSavings = 0;
    }
}
