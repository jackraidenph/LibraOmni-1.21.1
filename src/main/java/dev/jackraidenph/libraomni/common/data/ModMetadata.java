package dev.jackraidenph.libraomni.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;

import java.util.*;

public class ModMetadata {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String modId;

    private ModAnnotatedData annotatedData = null;
    private final Map<Scope, Set<String>> runtimeTasks = new HashMap<>();

    public ModMetadata(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public ModAnnotatedData getAnnotatedData() {
        return annotatedData;
    }

    public Set<String> runtimeTasksForScope(Scope scope) {
        Set<String> processors = this.runtimeTasks.get(scope);
        return processors == null ? Set.of() : Collections.unmodifiableSet(processors);
    }

    public void setAnnotatedData(ModAnnotatedData data) {
        this.annotatedData = data;
    }

    public void addRuntimeTasks(Scope scope, Collection<String> qualifiedNames) {
        this.runtimeTasks.computeIfAbsent(scope, k -> new HashSet<>()).addAll(qualifiedNames);
    }

    public static ModMetadata fromJson(String json) {
        return GSON.fromJson(json, ModMetadata.class);
    }

}
