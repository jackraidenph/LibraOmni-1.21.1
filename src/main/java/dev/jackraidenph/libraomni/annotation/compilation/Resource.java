package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record Resource(String directory, String name, String extension, byte[] bytes) {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .disableHtmlEscaping()
            .create();

    public Resource(String path, String name, String extension, String stringContents) {
        this(path, name, extension, stringContents.getBytes(StandardCharsets.UTF_8));
    }

    private static String assetDir(String modId) {
        return "assets/" + modId + "/";
    }

    private static String dataDir(String modId) {
        return "data/" + modId + "/";
    }

    public static Resource json(String directory, String name, Object jsonObject) {
        return utf8(directory, name, "json", GSON.toJson(jsonObject));
    }

    public static Resource png(String directory, String name, byte[] contents) {
        return new Resource(directory, name, "png", contents);
    }

    public static Resource png(String directory, String name, RenderedImage image) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            return new Resource(directory, name, "png", os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Resource binary(String directory, String name, String extension, byte[] bytes) {
        return new Resource(directory, name, extension, bytes);
    }

    public static Resource utf8(String directory, String name, String extension, String utf8String) {
        return binary(directory, name, extension, utf8String.getBytes(StandardCharsets.UTF_8));
    }

    public String baseName() {
        return this.name() + "." + this.extension();
    }

    public String path() {
        return this.directory() + this.name() + "." + this.extension();
    }
}
