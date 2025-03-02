package dev.jackraidenph.libraomni.util;

import dev.jackraidenph.libraomni.LibraOmni;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceUtilities {

    public static InputStream openResourceStream(String resourceLocation) {
        return LibraOmni.classLoader().getResourceAsStream(resourceLocation);
    }

    public static Stream<URL> getResources(String resourceLocation) {
        return LibraOmni.classLoader().resources(resourceLocation);
    }

    public static Stream<byte[]> getResourcesAsBytes(String resourceLocation) {
        return getResources(resourceLocation).map(url -> {
            try (InputStream is = url.openStream()) {
                return is.readAllBytes();
            } catch (IOException ioException) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public static Stream<String> getResourcesAsStrings(String resourceLocation) {
        return getResourcesAsBytes(resourceLocation).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }
}
