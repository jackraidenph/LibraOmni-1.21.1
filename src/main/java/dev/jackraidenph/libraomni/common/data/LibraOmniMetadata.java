package dev.jackraidenph.libraomni.common.data;

import com.google.gson.annotations.SerializedName;
import dev.jackraidenph.libraomni.LibraOmni;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LibraOmniMetadata {

    @SerializedName("mods")
    private final Map<String, ModMetadata> modMetadata = new HashMap<>();

    public static final String DIRECTORY = "META-INF/";
    public static final String FILE_ROOT = LibraOmni.MOD_ID + ".metadata";
    public static final String PATH = DIRECTORY + FILE_ROOT + ".json";

    public ModMetadata getOrCreateModMetadata(String modId) {
        return modMetadata.computeIfAbsent(modId, k -> new ModMetadata());
    }

    public void addModMetadata(String modId, ModMetadata modMetadata) {
        this.modMetadata.put(modId, modMetadata);
    }

    public Map<String, ModMetadata> getModMetadataMap() {
        return Collections.unmodifiableMap(modMetadata);
    }
}
