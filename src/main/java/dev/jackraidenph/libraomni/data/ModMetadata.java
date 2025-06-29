package dev.jackraidenph.libraomni.data;

import com.google.gson.annotations.SerializedName;

import java.util.*;

public class ModMetadata {
    @SerializedName("data")
    private final ModAnnotatedData annotatedData = new ModAnnotatedData();
    @SerializedName("tasks")
    private final Set<String> runtimeTasks = new HashSet<>();

    public ModAnnotatedData getAnnotatedData() {
        return annotatedData;
    }

    public Set<String> getTasks() {
        return Collections.unmodifiableSet(this.runtimeTasks);
    }

    public void addRuntimeTask(String taskClassName) {
        this.runtimeTasks.add(taskClassName);
    }
}
