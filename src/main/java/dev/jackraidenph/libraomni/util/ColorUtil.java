package dev.jackraidenph.libraomni.util;

import org.jetbrains.annotations.Range;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Most of the conversions present in this class do not preserve any form of alpha
 */
public final class ColorUtil {

    public static final float CIE_E = 216f / 24389f;
    public static final float CIE_K = 24389f / 27f;

    public static final float D65_X = 0.95047f;
    public static final float D65_Y = 1.00000f;
    public static final float D65_Z = 1.08883f;

    private ColorUtil() {
    }

    public static float[] argbToHSB(int argb) {
        int[] argbArr = decomposeARGB(argb);
        return Color.RGBtoHSB(argbArr[1], argbArr[2], argbArr[3], null);
    }

    public static int[] decomposeARGB(int color) {
        int[] components = new int[4];
        components[0] = (color >> 24) & 0xFF;
        components[1] = (color >> 16) & 0xFF;
        components[2] = (color >> 8) & 0xFF;
        components[3] = (color) & 0xFF;
        return components;
    }

    public static int composeARGB(int[] argb) {
        return (argb[0] << 24) + (argb[1] << 16) + (argb[2] << 8) + argb[3];
    }

    public static float toLinearIntensity(@Range(from = 0, to = 1) float value) {
        if (value <= 0.04045f) {
            return value * 0.0773993808f;
        }

        return (float) Math.pow(value * 0.9478672986f + 0.0521327014f, 2.4);
    }

    public static float toLinearIntensity(@Range(from = 0, to = 255) int colorComponent) {
        return toLinearIntensity(colorComponent / 255f);
    }

    public static float[] argbToLinear(int argb) {
        int[] decomposed = decomposeARGB(argb);
        return new float[]{
                toLinearIntensity(decomposed[1]),
                toLinearIntensity(decomposed[2]),
                toLinearIntensity(decomposed[3]),
        };
    }

    public static float[] argbToXYZ(int argb) {
        return linearRGBToXYZ(argbToLinear(argb));
    }

    public static float[] argbToLAB(int argb) {
        return xyzToLAB(argbToXYZ(argb));
    }

    public static float[] argbToOKLAB(int argb) {
        return xyzToOKLAB(argbToXYZ(argb));
    }

    public static float[] linearRGBToXYZ(float rL, float gL, float bL) {
        return new float[]{
                rL * 0.4124564f + gL * 0.3575761f + bL * 0.1804375f,
                rL * 0.2126729f + gL * 0.7151522f + bL * 0.0721750f,
                rL * 0.0193339f + gL * 0.1191920f + bL * 0.9503041f,
        };
    }

    public static float[] linearRGBToXYZ(float[] linearRGB) {
        return linearRGBToXYZ(linearRGB[0], linearRGB[1], linearRGB[2]);
    }

    private static float labTransfer(float t) {
        if (t > CIE_E) {
            return cbrtf(t);
        }

        return ((CIE_K * t) + 16) / 116.f;
    }

    public static float[] xyzToLAB(float x, float y, float z) {
        return new float[]{
                116 * labTransfer(y / D65_Y) - 16,
                500 * (labTransfer(x / D65_X) - labTransfer(y / D65_Y)),
                200 * (labTransfer(y / D65_Y) - labTransfer(z / D65_Z))
        };
    }

    public static float[] xyzToLAB(float[] xyz) {
        return xyzToLAB(xyz[0], xyz[1], xyz[2]);
    }

    private static float cbrtf(float v) {
        return (float) Math.cbrt(v);
    }

    public static float[] xyzToOKLAB(float x, float y, float z) {
        float l = 0.8189330101f * x + 0.3618667424f * y - 0.1288597137f * z;
        float m = 0.0329845436f * x + 0.9293118715f * y + 0.0361456387f * z;
        float s = 0.0482003018f * x + 0.2643662691f * y + 0.6338517070f * z;

        float ld = cbrtf(l);
        float md = cbrtf(m);
        float sd = cbrtf(s);

        return new float[]{
                0.2104542553f * ld + 0.7936177850f * md - 0.0040720468f * sd,
                1.9779984951f * ld - 2.4285922050f * md + 0.4505937099f * sd,
                0.0259040371f * ld + 0.7827717662f * md - 0.8086757660f * sd
        };
    }

    public static float[] xyzToOKLAB(float[] xyz) {
        return xyzToOKLAB(xyz[0], xyz[1], xyz[2]);
    }

    //Reverse functions

    public static float fromLinearIntensity(float linear) {
        if (linear <= 0.0031308f) {
            return 12.92f * linear;
        }

        return 1.055f * ((float) Math.pow(linear, 0.41666f)) - 0.055f;
    }

    /**
     * Returned alpha is 255
     */
    public static int hsbToARGB(float[] hsb) {
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) | 0xFF000000;
    }

    public static float[] labToXYZ(float L, float a, float b) {
        float fY = (L + 16) / 116f;
        float fX = (a / 500f) + fY;
        float fZ = fY - (b / 200f);

        float xR;
        if (fX * fX * fX > CIE_E) {
            xR = fX * fX * fX;
        } else {
            xR = (116f * fX - 16) / CIE_K;
        }

        float yR;
        if (L > (CIE_K * CIE_E)) {
            yR = fY * fY * fY;
        } else {
            yR = L / CIE_K;
        }

        float zR;
        if (fZ * fZ * fZ > CIE_E) {
            zR = fZ * fZ * fZ;
        } else {
            zR = (116f * fZ - 16) / CIE_K;
        }

        return new float[]{
                xR * D65_X,
                yR * D65_Y,
                zR * D65_Z,
        };
    }

    public static float[] xyzToLinearRGB(float x, float y, float z) {
        return new float[]{
                3.2404542f * x - 1.5371385f * y - 0.4985314f * z,
                -0.9692660f * x + 1.8760108f * y + 0.0415560f * z,
                0.0556434f * x - 0.2040259f * y + 1.0572252f * z
        };
    }

    /**
     * Returned alpha is 255
     */
    public static int linearToARGB(float[] linearRGB) {
        return composeARGB(new int[]{
                0xFF,
                Math.round(Math.clamp(fromLinearIntensity(linearRGB[0]), 0, 1) * 255),
                Math.round(Math.clamp(fromLinearIntensity(linearRGB[1]), 0, 1) * 255),
                Math.round(Math.clamp(fromLinearIntensity(linearRGB[2]), 0, 1) * 255)
        });
    }

    public static float[] xyzToLinearRGB(float[] xyz) {
        return xyzToLinearRGB(xyz[0], xyz[1], xyz[2]);
    }

    public static int xyzToARGB(float[] xyz) {
        return linearToARGB(xyzToLinearRGB(xyz[0], xyz[1], xyz[2]));
    }

    public static float[] labToXYZ(float[] Lab) {
        return labToXYZ(Lab[0], Lab[1], Lab[2]);
    }

    public static float[] labToLinearRGB(float L, float a, float b) {
        return xyzToLinearRGB(labToXYZ(L, a, b));
    }

    public static float[] labToLinearRGB(float[] Lab) {
        return xyzToLinearRGB(labToXYZ(Lab[0], Lab[1], Lab[2]));
    }

    public static int labToARGB(float L, float a, float b) {
        return linearToARGB(xyzToLinearRGB(labToXYZ(L, a, b)));
    }

    public static int oklabToARGB(float L, float a, float b) {
        return linearToARGB(xyzToLinearRGB(oklabToXYZ(L, a, b)));
    }

    public static int labToARGB(float[] Lab) {
        return linearToARGB(xyzToLinearRGB(labToXYZ(Lab[0], Lab[1], Lab[2])));
    }

    public static int oklabToARGB(float[] Lab) {
        return linearToARGB(xyzToLinearRGB(oklabToXYZ(Lab[0], Lab[1], Lab[2])));
    }

    public static float[] oklabToXYZ(float L, float a, float b) {
        float ld = L + 0.3963377774f * a + 0.2158037573f * b;
        float md = L - 0.1055613458f * a - 0.0638541728f * b;
        float sd = L - 0.0894841775f * a - 1.2914855480f * b;

        float l = ld * ld * ld;
        float m = md * md * md;
        float s = sd * sd * sd;

        return new float[]{
                1.227013851103521026f * l - 0.5577999806518222383f * m + 0.28125614896646780758f * s,
                -0.040580178423280593977f * l + 1.1122568696168301049f * m - 0.071676678665601200577f * s,
                -0.076381284505706892869f * l - 0.42148197841801273055f * m + 1.5861632204407947575f * s
        };
    }

    public static float[] oklabToXYZ(float[] Lab) {
        return oklabToXYZ(Lab[0], Lab[1], Lab[2]);
    }

    //Linear interpolation

    public static int lerpSRGB(int argb0, int argb1, float value) {
        return composeARGB(MathUtil.lerpArray(decomposeARGB(argb0), decomposeARGB(argb1), value));
    }

    public static int lerpLRGB(int argb0, int argb1, float value) {
        return linearToARGB(MathUtil.lerpArray(argbToLinear(argb0), argbToLinear(argb1), value));
    }

    public static int lerpLAB(int argb0, int argb1, float value) {
        return labToARGB(MathUtil.lerpArray(argbToLAB(argb0), argbToLAB(argb1), value));
    }

    public static int lerpOKLAB(int argb0, int argb1, float value) {
        return oklabToARGB(MathUtil.lerpArray(argbToOKLAB(argb0), argbToOKLAB(argb1), value));
    }

    public static int lerpXYZ(int argb0, int argb1, float value) {
        return xyzToARGB(MathUtil.lerpArray(argbToXYZ(argb0), argbToXYZ(argb1), value));
    }

    public static int lerpHSB(int argb0, int argb1, float value) {
        return hsbToARGB(MathUtil.lerpArray(argbToHSB(argb0), argbToHSB(argb1), value));
    }

    public static int interpolate(int argb0, int argb1, float value, InterpolationMode interpolationMode) {
        if (interpolationMode.equals(InterpolationMode.NONE)) {
            return argb0;
        }

        return switch (interpolationMode) {
            case LINEAR_SRGB -> ColorUtil.lerpSRGB(argb0, argb1, value);
            case LINEAR_LSRGB -> ColorUtil.lerpLRGB(argb0, argb1, value);
            case LINEAR_HSB -> ColorUtil.lerpHSB(argb0, argb1, value);
            case LINEAR_XYZ -> ColorUtil.lerpXYZ(argb0, argb1, value);
            case LINEAR_LAB -> ColorUtil.lerpLAB(argb0, argb1, value);
            case LINEAR_OKLAB -> ColorUtil.lerpOKLAB(argb0, argb1, value);
            default -> throw new IllegalArgumentException("Unexpected value [%s]".formatted(interpolationMode));
        };
    }

    // Distance functions

    public static double oklabDistSqr(int argb0, int argb1) {
        return MathUtil.arrayDistSqr(ColorUtil.argbToOKLAB(argb0), ColorUtil.argbToOKLAB(argb1));
    }

    public static double oklDistSqr(int argb0, int argb1) {
        float[] oklab0 = ColorUtil.argbToOKLAB(argb0);
        float[] oklab1 = ColorUtil.argbToOKLAB(argb1);

        float L0 = oklab0[0];
        float L1 = oklab1[0];

        return Math.abs((L1 - L0) * (L1 - L0));
    }

    public static double okabDistSqr(int argb0, int argb1) {
        float[] oklab0 = ColorUtil.argbToOKLAB(argb0);
        float[] oklab1 = ColorUtil.argbToOKLAB(argb1);

        float a0 = oklab0[1];
        float a1 = oklab1[1];

        float b0 = oklab0[1];
        float b1 = oklab1[1];

        return Math.hypot(a1 - a0, b1 - b0);
    }

    public static double xyzDistSqr(int argb0, int argb1) {
        return MathUtil.arrayDistSqr(ColorUtil.argbToXYZ(argb0), ColorUtil.argbToXYZ(argb1));
    }

    public static int findClosest(int closestTo, int[] argbVariants, BiFunction<Integer, Integer, Double> distFunc) {
        double dist = Double.MAX_VALUE;
        int closest = 0;
        for (int col : argbVariants) {
            double currDist = distFunc.apply(closestTo, col);
            if (currDist < dist) {
                dist = currDist;
                closest = col;
            }
        }
        return closest;
    }

    //

    public static Map<Integer, Integer> matchOKLAB(int[] palette0, int[] palette1) {
        Map<Integer, Integer> colors = new HashMap<>();

        boolean[] taken = new boolean[palette0.length];
        for (int newRgb : palette1) {
            double dist = Double.MAX_VALUE;
            int matchedRgb = 0, mIdx = 0;
            for (int i = 0; i < palette0.length; i++) {
                if (taken[i]) {
                    continue;
                }

                int oldRgb = palette0[i];
                double currDist = oklabDistSqr(oldRgb, newRgb);
                if (currDist < dist) {
                    dist = currDist;
                    matchedRgb = oldRgb;
                    mIdx = i;
                }
            }

            colors.put(matchedRgb, newRgb);
            taken[mIdx] = true;
        }

        return colors;
    }

    public static int[] interpolatePalette(int[] palette, int newSize, InterpolationMode interpolationMode) {
        if (interpolationMode.equals(InterpolationMode.NONE) || newSize <= palette.length) {
            return palette;
        }
        int[] interpolated = new int[newSize];

        int even = newSize / palette.length;
        int remainder = newSize % palette.length;

        for (int i = 0, j = 0; i < palette.length - 1; i++) {
            int c0 = palette[i];
            int c1 = palette[i + 1];

            int sampleSize = 1 + even;
            if (remainder > 0) {
                sampleSize++;
                remainder--;
            }
            sampleSize = Math.min(newSize - j, sampleSize);
            for (int k = 0; k < sampleSize; k++, j++) {
                float v = k / ((float) sampleSize);
                int iC = interpolate(c0, c1, v, interpolationMode);
                interpolated[j] = iC;
            }
        }
        interpolated[interpolated.length - 1] = palette[palette.length - 1];

        return interpolated;
    }

    public enum InterpolationMode {
        NONE,
        //Default sRGB
        LINEAR_SRGB,
        //Linear sRGB
        LINEAR_LSRGB,
        LINEAR_HSB,
        LINEAR_XYZ,
        //CIELAB
        LINEAR_LAB,
        //Generally recommended, yields the best perception results
        LINEAR_OKLAB,
    }
}
