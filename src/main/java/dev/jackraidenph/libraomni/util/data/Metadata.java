package dev.jackraidenph.libraomni.util.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;

import java.util.*;

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
        Set<String> processors = this.runtimeProcessors.get(scope);
        return processors == null ? Set.of() : Collections.unmodifiableSet(processors);
    }

    public void setElementDataPath(String path) {
        this.elementDataPath = path;
    }

    public void addRuntimeProcessors(Scope scope, String... qualifiedNames) {
        this.addRuntimeProcessors(scope, Arrays.asList(qualifiedNames));
    }

    public void addRuntimeProcessors(Scope scope, Collection<String> qualifiedNames) {
        this.runtimeProcessors.computeIfAbsent(scope, k -> new HashSet<>()).addAll(qualifiedNames);
    }

    public static Metadata fromJson(String json) {
        return GSON.fromJson(json, Metadata.class);
    }

}
