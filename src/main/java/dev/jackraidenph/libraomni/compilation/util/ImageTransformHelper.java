package dev.jackraidenph.libraomni.compilation.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.BiFunction;

import static dev.jackraidenph.libraomni.compilation.util.Resource.PNG_EXT;

public class ImageTransformHelper {
    public static final TintFunction OTHER_HUE = (dst, src) -> {
        int r0 = (dst >> 16) & 0xFF, g0 = (dst >> 8) & 0xFF, b0 = dst & 0xFF;
        int r1 = (dst >> 16) & 0xFF, g1 = (dst >> 8) & 0xFF, b1 = dst & 0xFF;

        float[] hsb0 = Color.RGBtoHSB(r0, g0, b0, null);
        float[] hsb1 = Color.RGBtoHSB(r1, g1, b1, null);
        hsb1[0] = hsb0[0];

        return Color.HSBtoRGB(hsb1[0], hsb1[1], hsb1[2]) | (dst & (0xFF000000)); //Restore alpha
    };

    public static Resource tintPng(Resource existing, Resource other) {
        return tintPng(existing, other, OTHER_HUE);
    }

    public static Resource tintPng(Resource existing, Resource other, TintFunction tintFunction) {
        if (existing == null) {
            return other;
        }

        BufferedImage image0 = imageFromResource(existing);
        BufferedImage image1 = imageFromResource(other);

        throwIfIncompatible(image0, image1);

        BufferedImage out = new BufferedImage(image0.getWidth(), image0.getHeight(), BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = out.getRaster();

        int width = image0.getWidth();
        int height = image0.getHeight();
        int[] pixels = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int existingARGB = image0.getRGB(x, y);
                int otherARGB = image1.getRGB(x, y);
                int tinted = tintFunction.apply(existingARGB, otherARGB);
                pixels[y * width + x] = tinted;
            }
        }

        raster.setDataElements(0, 0, width, height, pixels);

        return Resource.builder()
                .setNameRoot(existing.getNameRoot())
                .setDirectory(existing.getDirectory())
                .setPngContents(out)
                .build();
    }

    public static Resource overlayPng(Resource existing, Resource other) {
        if (existing == null) {
            return other;
        }

        BufferedImage image0 = imageFromResource(existing);
        BufferedImage image1 = imageFromResource(other);

        throwIfIncompatible(image0, image1);

        BufferedImage out = new BufferedImage(image0.getWidth(), image0.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = out.getGraphics();
        g.drawImage(image0, 0, 0, null);
        g.drawImage(image1, 0, 0, null);

        return Resource.builder()
                .setNameRoot(existing.getNameRoot())
                .setDirectory(existing.getDirectory())
                .setPngContents(out)
                .build();
    }

    private static BufferedImage createImageFromBytes(byte[] imageData) {
        ByteArrayInputStream stream = new ByteArrayInputStream(imageData);
        try {
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage imageFromResource(Resource resource) {
        if (!resource.getExtension().equals(PNG_EXT)) {
            throw new IllegalArgumentException("Tried to get BufferedImage from non-PNG resources");
        }

        BufferedImage img = createImageFromBytes(resource.getContents());
        if (img == null) {
            throw new IllegalStateException("Failed to create BufferedImage from [%s]".formatted(resource));
        }
        return img;
    }

    private static boolean checkDimensions(BufferedImage image0, BufferedImage image1) {
        int width0 = image0.getWidth();
        int height0 = image0.getHeight();

        return width0 == image1.getWidth() && height0 == image1.getHeight();
    }

    private static void throwIfIncompatible(BufferedImage image0, BufferedImage image1) {
        if (!checkDimensions(image0, image1)) {
            throw new IllegalArgumentException(
                    "Images dimensions are different, [%d, %d], [%d, %d]".formatted(image0.getWidth(), image0.getHeight(), image1.getWidth(), image1.getHeight())
            );
        }
    }

    @FunctionalInterface
    public interface TintFunction extends BiFunction<Integer, Integer, Integer> {
        Integer apply(Integer argbDst, Integer argbSrc);
    }
}
