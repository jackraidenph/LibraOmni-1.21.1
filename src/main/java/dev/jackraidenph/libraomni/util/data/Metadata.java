package dev.jackraidenph.libraomni.util.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Metadata {

    private static final Gson METADATA_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String modId;

    private String elementDataPath = null;
    private final Map<Scope, Set<String>> runtimeProcessors = new HashMap<>();

    public Metadata(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public String getElementDataPath() {
        return elementDataPath;
    }

    public Set<String> getRuntimeProcessors(Scope scope) {
        return runtimeProcessors.computeIfAbsent(scope, k -> new HashSet<>());
    }

    public void setElementDataPath(String path) {
        this.elementDataPath = path;
    }

    public void addRuntimeProcessorClass(Scope scope, String qualifiedName) {
        this.getRuntimeProcessors(scope).add(qualifiedName);
    }

    private static final String FILE_NAME_SUFFIX = "metadata";

    public static String fileRoot() {
        return LibraOmni.MODID + "." + FILE_NAME_SUFFIX;
    }

    public void output(OutputStream outputStream) throws IOException {
        String str = METADATA_GSON.toJson(this);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
    }

    public static Metadata deserialize(String str) {
        return METADATA_GSON.fromJson(str, Metadata.class);
    }

}
