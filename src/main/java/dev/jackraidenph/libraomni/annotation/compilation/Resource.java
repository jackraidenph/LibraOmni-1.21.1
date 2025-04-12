package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;

public record Resource(String name, String extension, byte[] bytes) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Resource(String name, String extension, String stringContents) {
        this(name, extension, stringContents.getBytes(StandardCharsets.UTF_8));
    }

    public static Resource json(String name, String jsonString) {
        return new Resource(name, "json", jsonString);
    }

    public static Resource json(String name, Object jsonObject) {
        return json(name, GSON.toJson(jsonObject));
    }

    public static Resource png(String name, byte[] contents) {
        return new Resource(name, "png", contents);
    }

    public static Resource fullName(String nameWithExtension, byte[] contents) {
        int dotIndex = nameWithExtension.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new IllegalArgumentException("Full file name must contain its extension");
        }

        String extension = nameWithExtension.substring(dotIndex + 1);
        String name = nameWithExtension.substring(0, dotIndex);

        return new Resource(name, extension, contents);
    }

    public static Resource fullName(String nameWithExtension, String contents) {
        return fullName(nameWithExtension, contents.getBytes(StandardCharsets.UTF_8));
    }

    public String fullName() {
        return this.name() + "." + this.extension();
    }
}
