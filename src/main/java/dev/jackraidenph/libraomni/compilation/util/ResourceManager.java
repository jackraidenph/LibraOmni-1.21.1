package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

public final class ResourceManager {

    private final ProcessingEnvironment pEnv;
    private final AnnotationProcessorConfig config;

    public ResourceManager(AnnotationProcessorConfig config, ProcessingEnvironment pEnv) {
        this.config = config;
        this.pEnv = pEnv;
    }

    public void save(ResourceIdentifier identifier, Object toSerialize) {
        save(new InMemoryResource(identifier, toSerialize));
    }

    public void save(ResourceIdentifier identifier, Object toSerialize, JsonMergeConflictPolicy forcedPolicy) {
        save(new InMemoryResource(identifier, toSerialize), forcedPolicy);
    }

    public void save(ResourceIdentifier identifier, byte[] bytes) {
        save(new InMemoryResource(identifier, bytes));
    }

    public void save(ResourceIdentifier identifier, byte[] bytes, JsonMergeConflictPolicy forcedPolicy) {
        save(new InMemoryResource(identifier, bytes), forcedPolicy);
    }

    public void save(InMemoryResource inMemoryResource) {
        save(inMemoryResource, null);
    }

    public void save(InMemoryResource inMemoryResource, JsonMergeConflictPolicy forcedPolicy) {
        Filer filer = pEnv.getFiler();
        Messager messager = pEnv.getMessager();

        messager.printNote("Saving resource [%s]".formatted(inMemoryResource));

        ResourceIdentifier resourceIdentifier = inMemoryResource.identifier();

        String origin = "Previously Generated";
        Optional<Path> pathToExisting = resourceIdentifier.atLocation(filer);
        if (pathToExisting.isEmpty()) {
            origin = "User Files";
            pathToExisting = resourceIdentifier.atLocation(config.getResourceSetDirs());
        }

        byte[] toWrite = inMemoryResource.data();

        if (pathToExisting.isEmpty()) {
            writeBytesToResourceLocation(resourceIdentifier, toWrite);
            return;
        }

        JsonMergeConflictPolicy policy = forcedPolicy != null
                ? forcedPolicy
                : getConflictPolicyFromConfig(inMemoryResource);

        switch (policy) {
            case THROW -> throw new IllegalStateException("Resource [%s] already exists".formatted(inMemoryResource));
            case OVERWRITE ->
                    pEnv.getMessager().printNote("Resource [%s] already exists [%s], overwriting".formatted(inMemoryResource, origin));
            case PREFER_NEW, PREFER_EXISTING -> {
                if (!resourceIdentifier.isJson()) {
                    pEnv.getMessager().printWarning("Can't process not-JSON resource [%s] with [%s] policy, overwriting".formatted(inMemoryResource, policy));
                    break;
                }

                pEnv.getMessager().printNote("Resource [%s] already exists [%s], merging with policy [%s]".formatted(inMemoryResource, origin, policy));
                String merged = handleMergeJson(inMemoryResource, policy, pathToExisting.get());
                toWrite = merged.getBytes();
            }
        }

        writeBytesToResourceLocation(resourceIdentifier, toWrite);
    }

    private JsonMergeConflictPolicy getConflictPolicyFromConfig(InMemoryResource inMemoryResource) {
        JsonMergeConflictPolicy conflictPolicy = config.getConflictPolicy(inMemoryResource.identifier());
        if (conflictPolicy == null) {
            pEnv.getMessager().printWarning("Conflict resolution policy for [%s] not found, assuming [%s]".formatted(inMemoryResource.identifier(), conflictPolicy));
            return JsonMergeConflictPolicy.OVERWRITE;
        }
        return conflictPolicy;
    }

    private void writeBytesToResourceLocation(ResourceIdentifier resourceIdentifier, byte[] bytes) {
        try (OutputStream outputStream = resourceIdentifier.outputStream(pEnv.getFiler())) {
            outputStream.write(bytes);
        } catch (IOException ioException) {
            throw new RuntimeException("Exception writing resource [" + resourceIdentifier + "]", ioException);
        }
    }

    private String handleMergeJson(InMemoryResource resource, JsonMergeConflictPolicy policy, Path systemPathToExistingFile) {
        ResourceIdentifier identifier = resource.identifier();

        String newData = new String(resource.data());
        if (policy.equals(JsonMergeConflictPolicy.OVERWRITE)) {
            return newData;
        }

        if (!identifier.getExtension().equals(ResourceIdentifier.JSON_EXT)) {
            pEnv.getMessager().printWarning("Can't process not-JSON resource [%s] with [%s] policy, overwriting");
            return newData;
        }

        String existing = new String(identifier.read(systemPathToExistingFile));
        return JsonMergeHelper.mergeJson(existing, newData, policy);
    }

}
