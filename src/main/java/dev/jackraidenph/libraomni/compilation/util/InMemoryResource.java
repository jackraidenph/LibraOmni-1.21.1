package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.common.CommonGson;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public record InMemoryResource(ResourceIdentifier identifier, byte[] data) {
    public InMemoryResource(ResourceIdentifier resourceIdentifier, Object object) {
        this(resourceIdentifier, CommonGson.DEFAULT.toJson(object));
    }

    public InMemoryResource(ResourceIdentifier resourceIdentifier, String str, Charset charset) {
        this(resourceIdentifier, str.getBytes(charset));
    }

    public InMemoryResource(ResourceIdentifier resourceIdentifier, String str) {
        this(resourceIdentifier, str.getBytes());
    }

    @Override
    public @NotNull String toString() {
        return identifier.toString() + "@" + data.length + "bytes";
    }
}
