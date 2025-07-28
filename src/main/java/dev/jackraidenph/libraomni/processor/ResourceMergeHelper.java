package dev.jackraidenph.libraomni.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jackraidenph.libraomni.common.CommonGson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import static dev.jackraidenph.libraomni.processor.Resource.JSON_EXT;
import static dev.jackraidenph.libraomni.processor.Resource.PNG_EXT;

public class ResourceMergeHelper {

    public static final TintFunction OTHER_HUE = (dst, src) -> {
        int r0 = (dst >> 16) & 0xFF, g0 = (dst >> 8) & 0xFF, b0 = dst & 0xFF;
        int r1 = (dst >> 16) & 0xFF, g1 = (dst >> 8) & 0xFF, b1 = dst & 0xFF;

        float[] hsb0 = Color.RGBtoHSB(r0, g0, b0, null);
        float[] hsb1 = Color.RGBtoHSB(r1, g1, b1, null);
        hsb1[0] = hsb0[0];

        return Color.HSBtoRGB(hsb1[0], hsb1[1], hsb1[2]) | (dst & (0xFF000000)); //Restore alpha
    };

    public static Resource mergePng(Resource existing, Resource other, ImageMergeConflictPolicy policy) {
        return mergePng(existing, other, policy, OTHER_HUE);
    }

    public static Resource mergePng(Resource existing, Resource other, ImageMergeConflictPolicy policy, TintFunction tintFunction) {
        if (existing == null) {
            return other;
        }

        if (policy.equals(ImageMergeConflictPolicy.THROW)) {
            throw new IllegalStateException("Tried merging two PNGs with THROW policy");
        }

        if (policy.equals(ImageMergeConflictPolicy.TINT) && tintFunction == null) {
            throw new NullPointerException("Tint function is null, while the policy is TINT");
        }

        if (!existing.getExtension().equals(PNG_EXT) || !other.getExtension().equals(PNG_EXT)) {
            throw new IllegalArgumentException("Tried to merge non-PNG resources");
        }

        BufferedImage image0 = createImageFromBytes(existing.getContents());
        if (image0 == null) {
            throw new IllegalStateException("Failed to create BufferedImage from [%s]".formatted(existing));
        }
        BufferedImage image1 = createImageFromBytes(other.getContents());
        if (image1 == null) {
            throw new IllegalStateException("Failed to create BufferedImage from [%s]".formatted(existing));
        }

        int width0 = image0.getWidth();
        int height0 = image0.getHeight();

        if (width0 != image1.getWidth() || height0 != image1.getHeight()) {
            throw new IllegalArgumentException(
                    "Images dimensions are different, [%s]->[%d, %d], [%s]->[%d, %d]"
                            .formatted(
                                    existing, width0, height0,
                                    other, image1.getWidth(), image1.getHeight()
                            )
            );
        }

        BufferedImage out = new BufferedImage(width0, height0, BufferedImage.TYPE_INT_ARGB);

        if (policy.equals(ImageMergeConflictPolicy.OVERLAY)) {
            Graphics g = out.getGraphics();
            g.drawImage(image0, 0, 0, null);
            g.drawImage(image1, 0, 0, null);
        } else {
            WritableRaster raster = out.getRaster();
            int[] pixels = new int[width0 * height0];
            for (int x = 0; x < width0; x++) {
                for (int y = 0; y < height0; y++) {
                    int existingARGB = image0.getRGB(x, y);
                    int otherARGB = image1.getRGB(x, y);
                    int tinted = tintFunction.apply(existingARGB, otherARGB);
                    pixels[y * width0 + x] = tinted;
                }
            }

            raster.setDataElements(0, 0, width0, height0, pixels);
        }

        return Resource.png(out)
                .name(existing.getName())
                .directory(existing.getDirectory())
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

    public static Resource mergeJson(Resource existing, Resource other, JsonMergeConflictPolicy policy) {
        if (existing == null) {
            return other;
        }

        if (!existing.getExtension().equals(JSON_EXT) || !other.getExtension().equals(JSON_EXT)) {
            throw new IllegalArgumentException("Tried to merge non-JSON resources");
        }

        String json0 = new String(existing.getContents(), StandardCharsets.UTF_8);
        String json1 = new String(other.getContents(), StandardCharsets.UTF_8);

        JsonObject obj0 = CommonGson.DEFAULT.fromJson(json0, JsonObject.class);
        JsonObject obj1 = CommonGson.DEFAULT.fromJson(json1, JsonObject.class);

        return Resource.json(mergeObjects(obj0, obj1, policy))
                .name(existing.getName())
                .directory(existing.getDirectory())
                .build();
    }

    private static JsonObject mergeObjects(JsonObject obj0, JsonObject obj1, JsonMergeConflictPolicy policy) {
        for (Entry<String, JsonElement> kvPair : obj1.entrySet()) {
            String rightKey = kvPair.getKey();
            JsonElement rightVal = kvPair.getValue();

            JsonElement left = obj0.get(rightKey);
            if (left == null) {
                obj0.add(rightKey, rightVal);
                continue;
            }

            if (policy.equals(JsonMergeConflictPolicy.THROW)) {
                throw new IllegalStateException("Key [%s] is already present".formatted(kvPair.getKey()));
            }

            if (left.isJsonArray()) {
                JsonArray leftArray = left.getAsJsonArray();
                if (rightVal.isJsonArray()) {
                    for (JsonElement e : rightVal.getAsJsonArray()) {
                        leftArray.add(e);
                    }
                } else {
                    leftArray.add(rightKey);
                }
                continue;
            }

            if (left.isJsonObject()) {
                if (!rightVal.isJsonObject()) {
                    throw new IllegalStateException(
                            "Tried to merge JsonObject with non-JsonObject [%s]: [%s], [%s]"
                                    .formatted(rightKey, left, rightVal)
                    );
                }

                obj0.remove(rightKey);
                obj0.add(rightKey, mergeObjects(left.getAsJsonObject(), rightVal.getAsJsonObject(), policy));
                continue;
            }

            if (policy.equals(JsonMergeConflictPolicy.PREFER_NEW)) {
                obj0.remove(rightKey);
                obj0.add(rightKey, rightVal);
            }
        }

        return obj0;
    }

    @FunctionalInterface
    public interface TintFunction extends BiFunction<Integer, Integer, Integer> {
        Integer apply(Integer argbDst, Integer argbSrc);
    }

    public enum ImageMergeConflictPolicy {
        THROW,
        OVERLAY,
        TINT
    }

    public enum JsonMergeConflictPolicy {
        THROW,
        PREFER_EXISTING,
        PREFER_NEW
    }

}
