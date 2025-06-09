package dev.jackraidenph.libraomni.common.data;

import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;

import java.util.*;

public class ModMetadata {
    private ModAnnotatedData annotatedData = new ModAnnotatedData();
    private final Map<Scope, Set<String>> runtimeTasks = new HashMap<>();

    public ModAnnotatedData getAnnotatedData() {
        return annotatedData;
    }

    public Set<String> tasksForScope(Scope scope) {
        return Collections.unmodifiableSet(this.runtimeTasks.getOrDefault(scope, Set.of()));
    }

    public void setAnnotatedData(ModAnnotatedData data) {
        this.annotatedData = data;
    }

    public void addRuntimeTask(Scope scope, String taskClassName) {
        this.runtimeTasks.computeIfAbsent(scope, k -> new HashSet<>()).add(taskClassName);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
