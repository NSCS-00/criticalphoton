package com.dlzstudio.criticalphoton.simd;

/**
 * SIMD 风格向量运算
 * 使用手动循环展开和数组优化模拟 SIMD
 * 目标：4 倍于标量运算性能
 */
public class VectorMath {
    
    // 预计算常量表
    private static final float[] SIN_TABLE;
    private static final float[] COS_TABLE;
    private static final float[] INV_SQRT_TABLE;
    
    static {
        SIN_TABLE = new float[360];
        COS_TABLE = new float[360];
        INV_SQRT_TABLE = new float[256];
        
        for (int i = 0; i < 360; i++) {
            double rad = Math.toRadians(i);
            SIN_TABLE[i] = (float) Math.sin(rad);
            COS_TABLE[i] = (float) Math.cos(rad);
        }
        
        // 快速倒数平方根表
        for (int i = 1; i < 256; i++) {
            INV_SQRT_TABLE[i] = (float) (1.0 / Math.sqrt(i));
        }
        INV_SQRT_TABLE[0] = Float.MAX_VALUE;
    }
    
    /**
     * 批量向量加法 (循环展开 4x)
     */
    public static void addVectors(float[] ax, float[] ay, float[] az,
                                   float[] bx, float[] by, float[] bz,
                                   float[] rx, float[] ry, float[] rz,
                                   int count) {
        int i = 0;
        int limit = count - (count % 4);
        
        // 展开循环 (4x)
        while (i < limit) {
            rx[i] = ax[i] + bx[i];
            ry[i] = ay[i] + by[i];
            rz[i] = az[i] + bz[i];
            
            rx[i+1] = ax[i+1] + bx[i+1];
            ry[i+1] = ay[i+1] + by[i+1];
            rz[i+1] = az[i+1] + bz[i+1];
            
            rx[i+2] = ax[i+2] + bx[i+2];
            ry[i+2] = ay[i+2] + by[i+2];
            rz[i+2] = az[i+2] + bz[i+2];
            
            rx[i+3] = ax[i+3] + bx[i+3];
            ry[i+3] = ay[i+3] + by[i+3];
            rz[i+3] = az[i+3] + bz[i+3];
            
            i += 4;
        }
        
        // 处理剩余
        while (i < count) {
            rx[i] = ax[i] + bx[i];
            ry[i] = ay[i] + by[i];
            rz[i] = az[i] + bz[i];
            i++;
        }
    }
    
    /**
     * 批量向量乘法 (标量)
     */
    public static void mulVectors(float[] ax, float[] ay, float[] az,
                                   float scalar,
                                   float[] rx, float[] ry, float[] rz,
                                   int count) {
        int i = 0;
        int limit = count - (count % 4);
        
        while (i < limit) {
            rx[i] = ax[i] * scalar;
            ry[i] = ay[i] * scalar;
            rz[i] = az[i] * scalar;
            
            rx[i+1] = ax[i+1] * scalar;
            ry[i+1] = ay[i+1] * scalar;
            rz[i+1] = az[i+1] * scalar;
            
            rx[i+2] = ax[i+2] * scalar;
            ry[i+2] = ay[i+2] * scalar;
            rz[i+2] = az[i+2] * scalar;
            
            rx[i+3] = ax[i+3] * scalar;
            ry[i+3] = ay[i+3] * scalar;
            rz[i+3] = az[i+3] * scalar;
            
            i += 4;
        }
        
        while (i < count) {
            rx[i] = ax[i] * scalar;
            ry[i] = ay[i] * scalar;
            rz[i] = az[i] * scalar;
            i++;
        }
    }
    
    /**
     * 快速平方根 (使用查找表)
     */
    public static float fastSqrt(float value) {
        if (value <= 0) return 0;
        int intBits = Float.floatToIntBits(value);
        int approx = 0x1FBD1DF5 + (intBits >>> 1);
        float x0 = Float.intBitsToFloat(approx);
        // 一次牛顿迭代
        return 0.5f * (x0 + value / x0);
    }
    
    /**
     * 快速倒数平方根 (Quake III 算法)
     */
    public static float fastInvSqrt(float value) {
        if (value <= 0) return 0;
        int i = Float.floatToIntBits(value);
        i = 0x5f3759df - (i >> 1);
        float x = Float.intBitsToFloat(i);
        return x * (1.5f - 0.5f * value * x * x);
    }
    
    /**
     * 查表正弦
     */
    public static float fastSin(float degrees) {
        int index = ((int) degrees) % 360;
        if (index < 0) index += 360;
        return SIN_TABLE[index];
    }
    
    /**
     * 查表余弦
     */
    public static float fastCos(float degrees) {
        int index = ((int) degrees) % 360;
        if (index < 0) index += 360;
        return COS_TABLE[index];
    }
    
    /**
     * 批量距离计算 (平方)
     */
    public static void batchDistanceSquared(float[] ax, float[] ay, float[] az,
                                             float targetX, float targetY, float targetZ,
                                             float[] results, int count) {
        int i = 0;
        int limit = count - (count % 4);
        
        while (i < limit) {
            float dx0 = ax[i] - targetX;
            float dy0 = ay[i] - targetY;
            float dz0 = az[i] - targetZ;
            results[i] = dx0*dx0 + dy0*dy0 + dz0*dz0;
            
            float dx1 = ax[i+1] - targetX;
            float dy1 = ay[i+1] - targetY;
            float dz1 = az[i+1] - targetZ;
            results[i+1] = dx1*dx1 + dy1*dy1 + dz1*dz1;
            
            float dx2 = ax[i+2] - targetX;
            float dy2 = ay[i+2] - targetY;
            float dz2 = az[i+2] - targetZ;
            results[i+2] = dx2*dx2 + dy2*dy2 + dz2*dz2;
            
            float dx3 = ax[i+3] - targetX;
            float dy3 = ay[i+3] - targetY;
            float dz3 = az[i+3] - targetZ;
            results[i+3] = dx3*dx3 + dy3*dy3 + dz3*dz3;
            
            i += 4;
        }
        
        while (i < count) {
            float dx = ax[i] - targetX;
            float dy = ay[i] - targetY;
            float dz = az[i] - targetZ;
            results[i] = dx*dx + dy*dy + dz*dz;
            i++;
        }
    }
    
    /**
     * 批量归一化向量
     */
    public static void batchNormalize(float[] x, float[] y, float[] z, int count) {
        for (int i = 0; i < count; i++) {
            float len = fastSqrt(x[i]*x[i] + y[i]*y[i] + z[i]*z[i]);
            if (len > 0.0001f) {
                float invLen = 1.0f / len;
                x[i] *= invLen;
                y[i] *= invLen;
                z[i] *= invLen;
            }
        }
    }
}
