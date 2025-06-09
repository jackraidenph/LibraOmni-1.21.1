package dev.jackraidenph.libraomni.common.data;

import dev.jackraidenph.libraomni.LibraOmni;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModMetadataReader {

    public static final String DIRECTORY = "META-INF/" + LibraOmni.MOD_ID + "/";

    private static final String METADATA_FILE_ROOT = LibraOmni.MOD_ID + ".metadata";
    private static final String ELEMENT_DATA_FILE_PREFIX = "elements";

    private final Map<String, ModMetadata> modMetadataCache = new HashMap<>();

    public static String metadataFileRoot() {
        return METADATA_FILE_ROOT;
    }

    public static String metadataFileName() {
        return metadataFileRoot() + ".json";
    }

    public static String metadataFilePath() {
        return DIRECTORY + metadataFileName();
    }

    public static String annotatedDataFileRoot(String modId) {
        return modId + "." + ELEMENT_DATA_FILE_PREFIX;
    }

    public static String annotatedDataFileName(String modId) {
        return annotatedDataFileRoot(modId) + ".json";
    }

    public static String annotatedDataFilePath(String modId) {
        return DIRECTORY + annotatedDataFileName(modId);
    }

    public ModMetadata readModData(String modId) {
        if (this.modMetadataCache.containsKey(modId)) {
            return this.modMetadataCache.get(modId);
        }

        return this.readAllModData()
                .stream()
                .filter(modData -> modData.getModId().equals(modId))
                .findFirst()
                .orElse(null);
    }

    public Set<ModMetadata> findModsWithAnnotatedData() {
        if (!this.modMetadataCache.isEmpty()) {
            return this.modMetadataCache.values()
                    .stream()
                    .filter(modData -> modData.getAnnotatedData() != null)
                    .collect(Collectors.toSet());
        }

        return this.readAllModData()
                .stream()
                .filter(modData -> modData.getAnnotatedData() != null)
                .collect(Collectors.toSet());
    }

    public Set<ModMetadata> readAllModData() {
        return getResourcesAsStrings(metadataFilePath())
                .map(ModMetadata::fromJson)
                .filter(Objects::nonNull)
                .peek(modMetadata -> this.modMetadataCache.put(modMetadata.getModId(), modMetadata))
                .collect(Collectors.toSet());
    }

    public ModAnnotatedData readAnnotatedData(String modId) {
        ModMetadata modMetadata = this.readModData(modId);
        if (modMetadata != null) {
            return modMetadata.getAnnotatedData();
        }
        return null;
    }

    private static ModAnnotatedData readFromLocation(String location) {
        try (InputStream byteInputStream = openResourceStream(location)) {
            String str = new String(byteInputStream.readAllBytes(), StandardCharsets.UTF_8);
            return ModAnnotatedData.fromJson(str);
        } catch (IOException e) {
            LibraOmni.LOGGER.error("Failed to read element data from [{}]", location);
            return null;
        }
    }

    private static ClassLoader classLoader() {
        return ModMetadataReader.class.getClassLoader();
    }

    private static InputStream openResourceStream(String resourceLocation) {
        return classLoader().getResourceAsStream(resourceLocation);
    }

    private static Stream<URL> getResources(String resourceLocation) {
        return ModMetadataReader.class.getClassLoader().resources(resourceLocation);
    }

    private static Stream<byte[]> getResourcesAsBytes(String resourceLocation) {
        return getResources(resourceLocation).map(url -> {
            try (InputStream is = url.openStream()) {
                return is.readAllBytes();
            } catch (IOException ioException) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    private static Stream<String> getResourcesAsStrings(String resourceLocation) {
        return getResourcesAsBytes(resourceLocation).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }
}
