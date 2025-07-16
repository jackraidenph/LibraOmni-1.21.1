package dev.jackraidenph.libraomni.data;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.CommonGson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ModMetadataReader {

    private ProjectMetadata projectMetadata = null;
    private boolean init = false;

    public void readMetadataFile() {
        try (InputStream inputStream = openResourceStream(ProjectMetadata.PATH)) {
            if (inputStream != null) {
                String nativeMetadataJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                this.projectMetadata = CommonGson.DEFAULT.fromJson(nativeMetadataJson, ProjectMetadata.class);
            } else {
                LibraOmni.LOGGER.error("Failed to fetch metadata file");
                this.projectMetadata = new ProjectMetadata();
            }

            init = true;
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private Map<String, ModMetadata> modMetadataMap() {
        if (!init) {
            throw new IllegalStateException("Reader was not initialized");
        }

        return this.projectMetadata.getModMetadataMap();
    }

    public ModMetadata getModMetadata(String modId) {
        return modMetadataMap().get(modId);
    }

    public Map<String, ModMetadata> getAllModMetadata() {
        return Collections.unmodifiableMap(modMetadataMap());
    }

    public Collection<String> getAllModsWithMetadata() {
        return modMetadataMap().keySet();
    }

    private static ClassLoader classLoader() {
        return ModMetadataReader.class.getClassLoader();
    }

    private static InputStream openResourceStream(String resourceLocation) {
        return classLoader().getResourceAsStream(resourceLocation);
    }
}
