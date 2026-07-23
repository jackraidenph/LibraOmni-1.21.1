package dev.jackraidenph.libraomni.compilation.task.cache;

import com.google.gson.annotations.SerializedName;
import dev.jackraidenph.libraomni.compilation.task.CompilationTask;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.exception.AlreadyInitializedException;
import dev.jackraidenph.libraomni.util.CommonGson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoundCache {

    @SerializedName("tasksCache")
    private final Map<String, TaskCache> taskCaches = new LinkedHashMap<>();

    private transient boolean built;

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }

    public void cacheFile(String taskName, ResourceIdentifier resourceIdentifier, byte[] bytes) {
        if (isBuilt()) {
            throw new AlreadyInitializedException();
        }
        taskCaches.computeIfAbsent(taskName, v -> new TaskCache()).cacheResource(resourceIdentifier, bytes);
    }

    public void add(CompilationTask task, Element element, AnnotationMirror mirror) {
        if (isBuilt()) {
            throw new AlreadyInitializedException();
        }

        int hash = task.hashStructure(element, mirror);
        String name = task.getClass().getName();
        taskCaches.computeIfAbsent(name, v -> new TaskCache()).add(mirror, hash);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveToTempDir(int round) {
        if (!isBuilt()) {
            throw new IllegalStateException("Round cache is not built yet");
        }

        try {
            File tempFolderFile = ProcessingCache.getTempFolder().toFile();

            if (!tempFolderFile.exists()) {
                tempFolderFile.mkdirs();
            }

            String fileName = makeCacheFileName(round);

            File cacheFile = makeCacheFile(round);
            if (cacheFile.exists()) {
                cacheFile.delete();
            }

            if (!cacheFile.createNewFile() || !cacheFile.setWritable(true)) {
                throw new IllegalStateException("Failed to create file [%s]".formatted(fileName));
            }

            String json = CommonGson.DEFAULT.toJson(this);
            try (Writer w = new FileWriter(cacheFile)) {
                w.write(json);
            }

            for (TaskCache t : this.taskCaches.values()) {
                t.writeResourceCache();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String makeCacheFileName(int round) {
        return "r%d.cache".formatted(round);
    }

    public static File makeCacheFile(int round) {
        return new File(ProcessingCache.getTempFolder() + "/" + makeCacheFileName(round));
    }

    public static void removeFromTempDir(int round) {
        File file = makeCacheFile(round);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static @Nullable RoundCache readFromTempDir(int round) {
        File file = makeCacheFile(round);
        if (!file.exists() || !file.canRead()) {
            return null;
        }

        try (Reader reader = new FileReader(file)) {
            return CommonGson.DEFAULT.fromJson(reader, RoundCache.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public @Nullable TaskCache getTaskCache(String taskName) {
        return taskCaches.get(taskName);
    }

    public @Nonnull TaskCache getOrCreateTaskCache(String taskName) {
        TaskCache cache = getTaskCache(taskName);
        if (cache == null) {
            cache = new TaskCache();
            taskCaches.put(taskName, cache);
        }
        return cache;
    }
}
