package dev.jackraidenph.libraomni.compilation.task.cache;

import com.google.gson.annotations.SerializedName;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ElementUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

public class TaskCache {

    public static final String REGENERATED_ORIGIN = "_regenerated_";

    //An overall hash of a type of an annotation
    @SerializedName("typeCache")
    private final Map<String, Integer> mirrorToCache = new LinkedHashMap<>();

    @SerializedName("outputs")
    private final List<String> outputs = new ArrayList<>();

    private transient final Map<ResourceIdentifier, byte[]> filesToCache = new HashMap<>();

    public void copyOutputs(TaskCache oldCache) {
        this.outputs.addAll(oldCache.outputs);
    }

    public boolean elementsUpToDate(TaskCache cache) {
        if (cache == null) {
            return false;
        }

        return this.mirrorToCache.equals(cache.mirrorToCache);
    }

    public void add(TypeElement typeElement, int hash) {
        String str = ElementUtil.Javac.binaryName(typeElement);

        if (!mirrorToCache.containsKey(str)) {
            mirrorToCache.put(str, hash);
            return;
        }

        int oldHash = mirrorToCache.get(str);
        int newHash = hash + oldHash;
        mirrorToCache.put(str, newHash);
    }

    public void cacheResource(ResourceIdentifier resourceIdentifier, byte[] bytes) {
        outputs.add(resourceIdentifier.getFilePath());
        filesToCache.put(resourceIdentifier, bytes);
    }

    public void writeResourceCache() {
        for (Entry<ResourceIdentifier, byte[]> e : filesToCache.entrySet()) {
            ResourceIdentifier resourceIdentifier = e.getKey();
            byte[] bytes = e.getValue();

            File file = ProcessingCache.getTempFolder().resolve(resourceIdentifier.getFilePath()).toFile();
            if (!file.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }
            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(bytes);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to cache file [%s]".formatted(resourceIdentifier), ex);
            }
        }
    }

    public void outputResourceCache(ResourceManager resourceManager) {
        Path tempDir = ProcessingCache.getTempFolder();
        for (String out : outputs) {
            ResourceIdentifier identifier = ResourceIdentifier.parse(out);

            File file = tempDir.resolve(identifier.getFilePath()).toFile();
            byte[] bytes = readBytes(file);

            resourceManager.saveAndCache(identifier, bytes, JsonMergeConflictPolicy.OVERWRITE, REGENERATED_ORIGIN);
        }
    }

    private static byte[] readBytes(File file) {
        if (!file.exists() || !file.canRead()) {
            throw new IllegalStateException("File [%s] does not exist or can't be read".formatted(file));
        }

        try (InputStream is = new FileInputStream(file)) {
            return is.readAllBytes();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read [%s]".formatted(file), ioException);
        }
    }

    public void add(AnnotationMirror annotationMirror, int hash) {
        TypeElement typeElement = AnnotationMirrorUtil.toTypeElement(annotationMirror);
        add(typeElement, hash);
    }
}
