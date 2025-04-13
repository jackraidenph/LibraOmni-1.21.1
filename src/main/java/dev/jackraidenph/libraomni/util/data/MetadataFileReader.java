package dev.jackraidenph.libraomni.util.data;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum MetadataFileReader {

    INSTANCE;

    public static final String FILE_ROOT = "META-INF/" + LibraOmni.MODID + "/";

    private final Map<String, Metadata> modMetadataCache = new HashMap<>();

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
        return ResourceUtilities.getResourcesAsStrings(MetadataFileReader.FILE_ROOT + Metadata.fileRoot() + ".json")
                .map(Metadata::deserialize)
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
        try (InputStream byteInputStream = ResourceUtilities.openResourceStream(location)) {
            String str = new String(byteInputStream.readAllBytes(), StandardCharsets.UTF_8);
            return ElementData.fromJson(str);
        } catch (IOException e) {
            LibraOmni.LOGGER.error("Failed to read element data from [{}]", location);
            return null;
        }
    }
}
