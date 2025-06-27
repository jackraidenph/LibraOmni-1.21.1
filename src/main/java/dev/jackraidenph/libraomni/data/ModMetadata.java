package dev.jackraidenph.libraomni.data;

import com.google.gson.annotations.SerializedName;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;

import java.util.*;

public class ModMetadata {
    @SerializedName("data")
    private final ModAnnotatedData annotatedData = new ModAnnotatedData();
    @SerializedName("tasks")
    private final Map<Scope, Set<String>> runtimeTasks = new HashMap<>();

    public ModAnnotatedData getAnnotatedData() {
        return annotatedData;
    }

    public Set<String> tasksForScope(Scope scope) {
        return Collections.unmodifiableSet(this.runtimeTasks.getOrDefault(scope, Set.of()));
    }

    public void addRuntimeTask(Scope scope, String taskClassName) {
        this.runtimeTasks.computeIfAbsent(scope, k -> new HashSet<>()).add(taskClassName);
    }
}
