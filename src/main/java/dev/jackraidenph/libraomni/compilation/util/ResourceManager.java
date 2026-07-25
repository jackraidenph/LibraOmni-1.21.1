package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.compilation.task.cache.ProcessingCache;
import dev.jackraidenph.libraomni.compilation.task.cache.TaskCache;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.ConflictPolicy;

import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

public final class ResourceManager {

    private final ProcessingContext context;
    private final ProcessingCache cache;

    public ResourceManager(ProcessingContext context, ProcessingCache cache) {
        this.context = context;
        this.cache = cache;
    }

    public void save(InMemoryResource inMemoryResource) {
        saveAndCache(inMemoryResource, null);
    }

    public void saveAndCache(InMemoryResource inMemoryResource, @Nullable String cacheOrigin) {
        save(inMemoryResource, null, cacheOrigin);
    }

    //---

    public void save(ResourceIdentifier identifier, Object toSerialize) {
        saveAndCache(identifier, toSerialize, null);
    }

    public void saveAndCache(ResourceIdentifier identifier, Object toSerialize, @Nullable String cacheOrigin) {
        saveAndCache(identifier, toSerialize, null, cacheOrigin);
    }

    public void save(ResourceIdentifier identifier, Object toSerialize, ConflictPolicy forcedPolicy) {
        saveAndCache(identifier, toSerialize, forcedPolicy, null);
    }

    public void saveAndCache(ResourceIdentifier identifier, Object toSerialize, ConflictPolicy forcedPolicy, @Nullable String cacheOrigin) {
        save(new InMemoryResource(identifier, toSerialize), forcedPolicy, cacheOrigin);
    }

    //---

    public void save(ResourceIdentifier identifier, byte[] bytes) {
        save(identifier, bytes, null);
    }

    public void save(ResourceIdentifier identifier, byte[] bytes, ConflictPolicy forcedPolicy) {
        saveAndCache(identifier, bytes, forcedPolicy, null);
    }

    public void saveAndCache(ResourceIdentifier identifier, byte[] bytes, @Nullable String cacheOrigin) {
        saveAndCache(identifier, bytes, null, cacheOrigin);
    }

    public void saveAndCache(ResourceIdentifier identifier, byte[] bytes, ConflictPolicy forcedPolicy, @Nullable String cacheOrigin) {
        save(new InMemoryResource(identifier, bytes), forcedPolicy, cacheOrigin);
    }

    //---

    public void save(InMemoryResource inMemoryResource, @Nullable ConflictPolicy forcedPolicy, @Nullable String cacheOrigin) {
        Filer filer = context.processingEnvironment().getFiler();

        ResourceIdentifier resourceIdentifier = inMemoryResource.identifier();
        byte[] toWrite = inMemoryResource.data();

        if (cacheOrigin != null && cacheOrigin.equals(TaskCache.REGENERATED_ORIGIN)) {
            writeBytesToResourceLocation(resourceIdentifier, toWrite, null);
            return;
        }

        Messager messager = context.processingEnvironment().getMessager();

        messager.printNote("Saving resource [%s]".formatted(inMemoryResource));

        String origin = "Previously Generated";
        Optional<Path> pathToExisting = resourceIdentifier.atLocation(filer);
        if (pathToExisting.isEmpty()) {
            origin = "User Files";
            pathToExisting = resourceIdentifier.atLocation(context.config().getResourceSetDirs());
        }

        if (pathToExisting.isEmpty()) {
            writeBytesToResourceLocation(resourceIdentifier, toWrite, cacheOrigin);
            return;
        }

        ConflictPolicy policy = forcedPolicy != null
                ? forcedPolicy
                : getConflictPolicyFromConfig(inMemoryResource);

        ProcessingEnvironment pEnv = context.processingEnvironment();
        switch (policy) {
            case THROW -> throw new IllegalStateException("Resource [%s] already exists".formatted(inMemoryResource));
            case OVERWRITE_FILE ->
                    pEnv.getMessager().printNote("Resource [%s] already exists [%s], overwriting".formatted(inMemoryResource, origin));
            case MERGE_KEYS_PREFER_NEW, MERGE_KEYS_PREFER_EXISTING -> {
                if (!resourceIdentifier.isJson()) {
                    pEnv.getMessager().printWarning("Can't process not-JSON resource [%s] with [%s] policy, overwriting".formatted(inMemoryResource, policy));
                    break;
                }

                pEnv.getMessager().printNote("Resource [%s] already exists [%s], merging with policy [%s]".formatted(inMemoryResource, origin, policy));
                String merged = handleMergeJson(inMemoryResource, policy, pathToExisting.get());
                toWrite = merged.getBytes();
            }
        }

        writeBytesToResourceLocation(resourceIdentifier, toWrite, cacheOrigin);
    }

    private ConflictPolicy getConflictPolicyFromConfig(InMemoryResource inMemoryResource) {
        ConflictPolicy conflictPolicy = context.config().getConflictPolicy(inMemoryResource.identifier());
        if (conflictPolicy == null) {
            conflictPolicy = ConflictPolicy.OVERWRITE_FILE;
            context.processingEnvironment()
                    .getMessager()
                    .printWarning("Conflict resolution policy for [%s] not found, assuming [%s]".formatted(inMemoryResource.identifier(), conflictPolicy));
            return conflictPolicy;
        }
        return conflictPolicy;
    }

    private void writeBytesToResourceLocation(ResourceIdentifier resourceIdentifier, byte[] bytes, @Nullable String cacheOrigin) {
        try (OutputStream outputStream = resourceIdentifier.outputStream(context.processingEnvironment().getFiler())) {
            outputStream.write(bytes);

            if (cacheOrigin != null) {
                cacheBytes(cacheOrigin, resourceIdentifier, bytes);
            }

            writeToGradleExclusionList(resourceIdentifier);
        } catch (IOException ioException) {
            throw new RuntimeException("Exception writing resource [" + resourceIdentifier + "]", ioException);
        }
    }

    public static final File GRADLE_EXCLUSION_LIST_FILE = ProcessingCache.getTempFolder().resolve("gradleExclusionList").toFile();

    public static void clearGradleExclusionListFile() {
        if (!GRADLE_EXCLUSION_LIST_FILE.exists()) {
            return;
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(GRADLE_EXCLUSION_LIST_FILE, "rw")) {
            randomAccessFile.setLength(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeToGradleExclusionList(ResourceIdentifier resourceIdentifier) throws IOException {
        File exclusionList = GRADLE_EXCLUSION_LIST_FILE;
        if (!exclusionList.getParentFile().exists()) {
            exclusionList.getParentFile().mkdirs();
        }

        if (!exclusionList.exists()) {
            exclusionList.createNewFile();
            exclusionList.setWritable(true);
            exclusionList.setReadable(true);
        }

        try (FileWriter fileWriter = new FileWriter(exclusionList, true)) {
            fileWriter.write(resourceIdentifier.toString());
            fileWriter.write(';');
        }
    }

    private void cacheBytes(String taskName, ResourceIdentifier resourceIdentifier, byte[] bytes) {
        cache.getOrCreateRoundCache(context.round()).cacheFile(taskName, resourceIdentifier, bytes);
    }

    private String handleMergeJson(InMemoryResource resource, ConflictPolicy policy, Path systemPathToExistingFile) {
        ResourceIdentifier identifier = resource.identifier();

        String newData = new String(resource.data());
        if (policy.equals(ConflictPolicy.OVERWRITE_FILE)) {
            return newData;
        }

        if (!identifier.getExtension().equals(ResourceIdentifier.JSON_EXT)) {
            context.processingEnvironment()
                    .getMessager()
                    .printWarning("Can't process not-JSON resource [%s] with [%s] policy, overwriting");
            return newData;
        }

        String existing = new String(identifier.read(systemPathToExistingFile));
        return JsonMergeHelper.mergeJson(existing, newData, policy);
    }

}
