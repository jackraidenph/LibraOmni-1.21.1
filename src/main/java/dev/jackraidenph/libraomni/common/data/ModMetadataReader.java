package dev.jackraidenph.libraomni.common.data;

import dev.jackraidenph.libraomni.common.CommonGson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ModMetadataReader {

    private LibraOmniMetadata libraOmniMetadata = null;
    private boolean init = false;

    public void init() {
        try (InputStream inputStream = openResourceStream(LibraOmniMetadata.PATH)) {
            String nativeMetadataJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            this.libraOmniMetadata = CommonGson.DEFAULT.fromJson(nativeMetadataJson, LibraOmniMetadata.class);
            init = true;
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private Map<String, ModMetadata> modMetadataMap() {
        if (!init) {
            throw new IllegalStateException("Reader was not initialized");
        }

        return this.libraOmniMetadata.getModMetadataMap();
    }

    public ModMetadata getModMetadata(String modId) {
        if (!init) {
            throw new IllegalStateException("Reader was not initialized");
        }

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
