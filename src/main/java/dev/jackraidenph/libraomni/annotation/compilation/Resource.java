package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record Resource(String directory, String name, String extension, byte[] bytes) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Resource(String path, String name, String extension, String stringContents) {
        this(path, name, extension, stringContents.getBytes(StandardCharsets.UTF_8));
    }

    public static Resource json(String path, String name, Object jsonObject) {
        return utf8(path + name + ".json", GSON.toJson(jsonObject));
    }

    public static Resource png(String path, String name, byte[] contents) {
        return new Resource(path, name, "png", contents);
    }

    public static Resource png(String path, String name, RenderedImage image) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            return new Resource(path, name, "png", os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Resource binary(String path, byte[] contents) {
        int dotIndex = path.lastIndexOf(".");
        int slashIndex = path.lastIndexOf("/");
        if (dotIndex < 0 || slashIndex < 0) {
            throw new IllegalArgumentException("Malformed path, must be of pattern: path0/.../name.extension");
        }

        String extension = path.substring(dotIndex + 1);
        String name = path.substring(slashIndex + 1, dotIndex);
        String directory = path.substring(0, slashIndex + 1);

        return new Resource(directory, name, extension, contents);
    }

    public static Resource utf8(String path, String contents) {
        return binary(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    public String baseName() {
        return this.name() + "." + this.extension();
    }

    public String path() {
        return this.directory() + this.name() + "." + this.extension();
    }
}
