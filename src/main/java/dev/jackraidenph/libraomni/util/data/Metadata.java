package dev.jackraidenph.libraomni.util.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Metadata {

    private static final Gson GSON = new GsonBuilder()
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

    public static Metadata fromJson(String json) {
        return GSON.fromJson(json, Metadata.class);
    }

}
