package dev.jackraidenph.libraomni.util;

import dev.jackraidenph.libraomni.LibraOmni;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ResourceUtilities {

    public static final String ANNOTATION_MAP_FILE_SUFFIX = ".annotation_map.json";
    public static final String ANNOTATION_MAP_REGISTRY_FILE_SUFFIX = ".annotation_map.registry";
    public static final String ANNOTATION_MAP_REGISTRY_FILE = LibraOmni.MODID + ANNOTATION_MAP_REGISTRY_FILE_SUFFIX;

    public static String annotationMapLocationForMod(String modId) {
        return LibraOmni.MODID + "/" + modId + ResourceUtilities.ANNOTATION_MAP_FILE_SUFFIX;
    }

    public static InputStream openResourceStream(String resourceLocation) {
        return LibraOmni.classLoader().getResourceAsStream(resourceLocation);
    }

    public static Stream<URL> getResources(String resourceLocation) {
        return LibraOmni.classLoader().resources(resourceLocation);
    }

    public static Set<String> gatherAnnotationMaps() {
        final String registryLocation = LibraOmni.MODID + "/" + ANNOTATION_MAP_REGISTRY_FILE;
        final Set<String> maps = new HashSet<>();

        getResources(registryLocation).forEach(url -> {
            try (InputStream inputStream = url.openStream()) {
                String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                maps.addAll(Set.of(contents.split("\n")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return maps;
    }

    public static String extractModIdFromAnnotationMapName(String name) {
        return name.split("\\.annotationMap")[0];
    }
}
