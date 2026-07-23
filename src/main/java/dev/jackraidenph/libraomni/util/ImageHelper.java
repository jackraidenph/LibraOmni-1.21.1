package dev.jackraidenph.libraomni.util;

import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.util.ColorUtil.InterpolationMode;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;

import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class ImageHelper {

    private ImageHelper() {

    }

    public static BufferedImage recolor(BufferedImage originalImage, int[] newPalette, InterpolationMode interpolationMode) {
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();

        int[] originalPalette = IntStream.of(originalImage.getRGB(0, 0, w, h, null, 0, w))
                .filter(argb -> (argb & 0xFF000000) != 0) //Ignore fully transparent
                .distinct() //Palette of colors
                .toArray();

        final int originalPaletteLength = originalPalette.length;

        int[] newColors = interpolationMode.equals(InterpolationMode.NONE)
                ? newPalette
                : ColorUtil.interpolatePalette(newPalette, originalPaletteLength, interpolationMode);

        if (originalPaletteLength > newColors.length) {
            throw new IllegalArgumentException("Palette %s has [%d] colors, which is less than the original texture's [%d]".formatted(Arrays.toString(newPalette), newPalette.length, originalPaletteLength));
        }

        Map<Integer, Integer> colors = ColorUtil.matchOKLAB(originalPalette, newColors);

        return remapColors(originalImage, colors);
    }

    public static BufferedImage remapColors(BufferedImage originalImage, Map<Integer, Integer> colors) {
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();

        BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        IntStream pixelStream = IntStream.of(originalImage.getRGB(0, 0, w, h, null, 0, w));
        if (w * h >= (2 << 17)) { //512 x 512 = 262144
            //noinspection ResultOfMethodCallIgnored
            pixelStream.parallel();
        }
        int[] arr = pixelStream.map(oldRgb -> {
                    if ((oldRgb & 0xFF000000) == 0) {
                        return oldRgb;
                    }
                    return colors.getOrDefault(oldRgb, oldRgb) | (oldRgb & 0xFF000000);
                })
                .toArray();

        newImage.setRGB(0, 0, w, h, arr, 0, w);
        return newImage;
    }

    public static InMemoryResource transformPng(
            String namespace,
            String directory,
            String file,
            Function<BufferedImage, BufferedImage> transform,
            ProcessingContext processingContext
    ) {
        return transformPng(namespace, directory, file, null, transform, processingContext);
    }

    public static InMemoryResource transformPng(
            ResourceIdentifier textureLocation,
            Function<BufferedImage, BufferedImage> transform,
            ProcessingContext processingContext
    ) {
        return transformPng(textureLocation, null, transform, processingContext);
    }

    public static InMemoryResource transformPng(
            String namespace,
            String directory,
            String file,
            @Nullable ResourceIdentifier saveLocationOverride,
            Function<BufferedImage, BufferedImage> transform,
            ProcessingContext processingContext
    ) {
        return transformPng(
                ResourceIdentifier.pngAsset(namespace, directory, file),
                saveLocationOverride,
                transform,
                processingContext
        );
    }

    public static InMemoryResource transformPng(
            ResourceIdentifier textureLocation,
            @Nullable ResourceIdentifier saveLocationOverride,
            Function<BufferedImage, BufferedImage> transform,
            ProcessingContext processingContext
    ) {
        ResourceManager resourceManager = processingContext.resourceManager();
        Filer filer = processingContext.processingEnvironment().getFiler();
        Optional<Path> existingTexture = textureLocation.atLocation(filer);
        if (existingTexture.isEmpty()) {
            existingTexture = textureLocation.atLocation(processingContext.config().getResourceSetDirs());
        }
        if (existingTexture.isEmpty()) {
            throw new IllegalStateException("Failed to find [%s]".formatted(textureLocation));
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(existingTexture.get().toFile());
            BufferedImage newImage = transform.apply(image);
            ImageIO.write(newImage, "png", outputStream);

            byte[] bytes = outputStream.toByteArray();
            ResourceIdentifier identifier = saveLocationOverride == null ? textureLocation : saveLocationOverride;
            return new InMemoryResource(identifier, bytes);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
