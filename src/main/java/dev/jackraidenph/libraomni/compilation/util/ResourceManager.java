package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

public final class ResourceManager {

    private final ProcessingEnvironment pEnv;
    private final AnnotationProcessorConfig config;

    public ResourceManager(AnnotationProcessorConfig config, ProcessingEnvironment pEnv) {
        this.config = config;
        this.pEnv = pEnv;
    }

    public void save(ResourceIdentifier identifier, String jsonString) {
        save(new InMemoryResource(identifier, jsonString));
    }

    public void save(ResourceIdentifier identifier, String jsonString, Charset charset) {
        save(new InMemoryResource(identifier, jsonString, charset));
    }

    public void save(ResourceIdentifier identifier, Object toSerialize) {
        save(new InMemoryResource(identifier, toSerialize));
    }

    public void save(InMemoryResource inMemoryResource) {
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

        if (pathToExisting.isPresent()) {
            JsonMergeConflictPolicy policy = config.getConflictPolicy(inMemoryResource.identifier());
            if (policy == null) {
                policy = JsonMergeConflictPolicy.OVERWRITE;
                pEnv.getMessager().printWarning("Conflict resolution policy for [%s] not found, assuming [%s]".formatted(inMemoryResource.identifier(), policy));
            }

            if (!resourceIdentifier.isJson()) {
                throw new UnsupportedOperationException("Resource [%s] already exists, can't resolve the conflict for non-JSON resources");
            } else if (policy.equals(JsonMergeConflictPolicy.THROW)) {
                throw new IllegalStateException("Resource [%s] already exists".formatted(inMemoryResource));
            }

            if (!policy.equals(JsonMergeConflictPolicy.OVERWRITE)) {
                pEnv.getMessager().printNote("Resource [%s] already exists [%s], merging with policy [%s]".formatted(inMemoryResource, origin, policy));
                String merged = handleMergeJson(inMemoryResource, policy, pathToExisting.get());
                toWrite = merged.getBytes();
            } else {
                pEnv.getMessager().printNote("Resource [%s] already exists [%s], overwriting".formatted(inMemoryResource, origin));
            }
        }

        writeBytesToResourceLocation(resourceIdentifier, toWrite);
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
        if (!identifier.getExtension().equals(ResourceIdentifier.JSON_EXT)) {
            if (policy.equals(JsonMergeConflictPolicy.OVERWRITE)) {
                return newData;
            } else {
                throw new IllegalStateException("Can't process resource [%s] with policy [%s]".formatted(resource, policy));
            }
        }

        String existing = new String(identifier.read(systemPathToExistingFile));
        return JsonMergeHelper.mergeJson(existing, newData, policy);
    }

}
