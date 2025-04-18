package dev.jackraidenph.libraomni.util.data;

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

public enum MetadataFileReader {

    INSTANCE;

    public static final String DIRECTORY = "META-INF/" + LibraOmni.MODID + "/";

    private static final String METADATA_FILE_ROOT = LibraOmni.MODID + ".metadata";
    private static final String ELEMENT_DATA_FILE_PREFIX = "elements";

    private final Map<String, Metadata> modMetadataCache = new HashMap<>();

    public static String metadataFileRoot() {
        return METADATA_FILE_ROOT;
    }

    public static String metadataFileName() {
        return metadataFileRoot() + ".json";
    }

    public static String metadataFilePath() {
        return DIRECTORY + metadataFileName();
    }

    public static String elementDataFileRoot(String modId) {
        return modId + "." + ELEMENT_DATA_FILE_PREFIX;
    }

    public static String elementDataFileName(String modId) {
        return elementDataFileRoot(modId) + ".json";
    }

    public static String elementDataFilePath(String modId) {
        return DIRECTORY + elementDataFileName(modId);
    }

    public Metadata readModData(String modId) {
        if (this.modMetadataCache.containsKey(modId)) {
            return this.modMetadataCache.get(modId);
        }

        return this.readAllModData()
                .stream()
                .filter(modData -> modData.getModId().equals(modId))
                .findFirst()
                .orElse(null);
    }

    public Set<Metadata> findModsWithElementData() {
        if (!this.modMetadataCache.isEmpty()) {
            return this.modMetadataCache.values()
                    .stream()
                    .filter(modData -> modData.getElementDataPath() != null)
                    .collect(Collectors.toSet());
        }

        return this.readAllModData()
                .stream()
                .filter(modData -> modData.getElementDataPath() != null)
                .collect(Collectors.toSet());
    }

    public Set<Metadata> readAllModData() {
        return getResourcesAsStrings(metadataFilePath())
                .map(Metadata::fromJson)
                .filter(Objects::nonNull)
                .peek(metadata -> this.modMetadataCache.put(metadata.getModId(), metadata))
                .collect(Collectors.toSet());
    }

    public ElementData readElementData(String modId) {
        Metadata metadata = this.readModData(modId);
        if (metadata != null) {
            return readElementDataFromLocation(metadata.getElementDataPath());
        }
        return null;
    }

    private static ElementData readElementDataFromLocation(String location) {
        try (InputStream byteInputStream = openResourceStream(location)) {
            String str = new String(byteInputStream.readAllBytes(), StandardCharsets.UTF_8);
            return ElementData.fromJson(str);
        } catch (IOException e) {
            LibraOmni.LOGGER.error("Failed to read element data from [{}]", location);
            return null;
        }
    }

    private static ClassLoader classLoader() {
        return MetadataFileReader.class.getClassLoader();
    }

    private static InputStream openResourceStream(String resourceLocation) {
        return classLoader().getResourceAsStream(resourceLocation);
    }

    private static Stream<URL> getResources(String resourceLocation) {
        return MetadataFileReader.class.getClassLoader().resources(resourceLocation);
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
