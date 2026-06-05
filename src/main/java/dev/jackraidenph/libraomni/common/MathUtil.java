package dev.jackraidenph.libraomni.common;

public final class MathUtil {
    private MathUtil() {

    }

    public static float lerp(float f0, float f1, float value) {
        return f0 * (1 - value) + f1 * value;
    }

    public static float[] lerpArray(float[] from, float[] to, float value) {
        if (from.length != to.length) {
            throw new IllegalArgumentException("Arrays must be of the same size");
        }

        float[] res = new float[from.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = lerp(from[i], to[i], value);
        }

        return res;
    }

    public static int[] lerpArray(int[] from, int[] to, float value) {
        if (from.length != to.length) {
            throw new IllegalArgumentException("Arrays must be of the same size");
        }

        int[] res = new int[from.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (int) lerp(from[i], to[i], value);
        }
        return res;
    }

    public static double arrayDistSqr(float[] from, float[] to) {
        if (from.length != to.length) {
            throw new IllegalArgumentException("Arrays must be of the same size");
        }

        double sum = 0;
        for (int i = 0; i < from.length; i++) {
            sum += (to[i] * to[i]) - (from[i] * from[i]);
        }

        return Math.abs(sum);
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
