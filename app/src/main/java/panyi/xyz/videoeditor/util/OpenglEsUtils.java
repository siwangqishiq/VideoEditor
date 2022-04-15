package panyi.xyz.videoeditor.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OpenglEsUtils {
    private static final String TAG = "OpenglEsUtils";

    public static long framePerSecond = 0;
    public static long lastTime = 0;

    public static void debugFps() {
        framePerSecond++;
        long curTime = System.currentTimeMillis();
        if (curTime - lastTime >= 1000) {
            lastTime = curTime;
            //System.out.println("fps = " +framePerSecond);
            LogUtil.L(TAG , "fps = " +framePerSecond);
            framePerSecond = 0;
        }
    }

    public static FloatBuffer allocateBuf(float array[]) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        FloatBuffer buf = bb.asFloatBuffer();
        buf.put(array);
        buf.position(0);
        return buf;
    }

    public static ByteBuffer allocateBuf(byte array[]) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * Byte.BYTES)
                .order(ByteOrder.nativeOrder());
        bb.put(array);
        bb.position(0);
        return bb;
    }

    public static float[] convertColor(float r, float g, float b, int a) {
        float[] colors = new float[4];
        colors[0] = clamp(0.0f, 1.0f, r / 255);
        colors[1] = clamp(0.0f, 1.0f, g / 255);
        colors[2] = clamp(0.0f, 1.0f, b / 255);
        colors[3] = clamp(0.0f, 1.0f, a / 255);
        return colors;
    }

    public static void convertColor(float r, float g, float b, float a, float[] colors) {
        colors[0] = clamp(0.0f, 1.0f, r / 255);
        colors[1] = clamp(0.0f, 1.0f, g / 255);
        colors[2] = clamp(0.0f, 1.0f, b / 255);
        colors[3] = clamp(0.0f, 1.0f, a / 255);
    }

    public static float clamp(float min, float max, float v) {
        if (v <= min)
            return min;
        if (v >= max)
            return max;
        return v;
    }
}
