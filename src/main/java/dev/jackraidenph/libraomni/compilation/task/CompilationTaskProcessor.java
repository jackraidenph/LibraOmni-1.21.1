package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.service.ModPackage;
import dev.jackraidenph.libraomni.compilation.util.*;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private final Set<CompilationTask> tasks = new HashSet<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final AnnotationProcessorConfig config = new AnnotationProcessorConfig();

    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        config.init(processingEnv);
        RegisteredCompilationTask.init(this);
    }

    void registerTask(CompilationTask task) {
        if (this.tasks.stream().map(Object::getClass).anyMatch(clazz -> clazz.equals(task.getClass()))) {
            return;
        }

        this.tasks.add(task);
        if (processingEnv != null) {
            processingEnv.getMessager().printNote("Registered [" + task.getClass().getSimpleName() + "] for processing");
        }
    }

    private void findMods(RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME);
        TypeElement modRootAnnotation = this.processingEnv.getElementUtils().getTypeElement(ModPackage.class.getName());
        this.modIdGetter.findMods(modAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        this.modIdGetter.findMods(modRootAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        findMods(roundEnvironment);

        Messager messager = this.processingEnv.getMessager();
        boolean finishing = roundEnvironment.processingOver();

        if (!finishing) {
            messager.printNote("Processing round " + round);
        }

        Set<InMemoryResource> createdResources = new HashSet<>();

        for (CompilationTask compilationTask : this.tasks) {
            final String op = finishing ? "Finishing" : "Processing";

            messager.printNote(op + " [" + compilationTask.getClass().getSimpleName() + "]");

            try {
                RoundEnvironment proxyEnvironment = ProxyFactory.proxifyRuntimeEnvironment(roundEnvironment, processingEnv);
                Collection<InMemoryResource> output = !finishing
                        ? compilationTask.processRound(modIdGetter, proxyEnvironment, this.processingEnv)
                        : compilationTask.finish(modIdGetter, proxyEnvironment, this.processingEnv);
                createdResources.addAll(output);
            } catch (Exception e) {
                printStackTrace(e);
                throw new RuntimeException("Exception thrown while processing [%s]".formatted(compilationTask.getClass().getSimpleName()), e);
            }
        }

        if (!createdResources.isEmpty()) {
            messager.printNote("Saving resources " + createdResources);
        }

        for (InMemoryResource inMemoryResource : createdResources) {
            processAndSaveResource(inMemoryResource);
        }

        this.round++;
        return false;
    }

    private void processAndSaveResource(InMemoryResource inMemoryResource) {
        Filer filer = processingEnv.getFiler();

        ResourceIdentifier resourceIdentifier = inMemoryResource.resourceIdentifier();

        Optional<Path> pathToExisting = resourceIdentifier.existsAt(filer).or(() -> resourceIdentifier.existsAt(config.getResourceSetDirs()));
        if (pathToExisting.isPresent()) {

            JsonMergeConflictPolicy policy = getConflictPolicy(resourceIdentifier);

            if (policy == null) {
                policy = JsonMergeConflictPolicy.OVERWRITE;
                processingEnv.getMessager().printWarning("Failed to get conflict resolution policy for [%s], assuming [%s]".formatted(resourceIdentifier, policy));
            }

            if (!resourceIdentifier.getExtension().equals(ResourceIdentifier.JSON_EXT)) {
                if (policy.equals(JsonMergeConflictPolicy.OVERWRITE)) {
                    saveResource(resourceIdentifier, inMemoryResource.data());
                    return;
                } else {
                    throw new IllegalStateException("Can't process resource [" + inMemoryResource + "] with policy [" + policy + "]");
                }
            }

            String existing = new String(resourceIdentifier.read(pathToExisting.get()));
            String generated = new String(inMemoryResource.data());

            String merged = JsonMergeHelper.mergeJson(existing, generated, policy);

            processingEnv.getMessager().printNote("Resource [%s] already exists, merging with policy [%s]".formatted(inMemoryResource, policy));

            saveResource(resourceIdentifier, merged.getBytes());
            return;
        }

        saveResource(resourceIdentifier, inMemoryResource.data());
    }

    private void saveResource(ResourceIdentifier resourceIdentifier, byte[] bytes) {
        try (OutputStream outputStream = resourceIdentifier.outputStream(processingEnv.getFiler())) {
            outputStream.write(bytes);
        } catch (IOException ioException) {
            throw new RuntimeException("Exception writing resource [" + resourceIdentifier + "]", ioException);
        }
    }

    private JsonMergeConflictPolicy getConflictPolicy(ResourceIdentifier resourceIdentifier) {
        JsonMergeConflictPolicy policy = getConflictPolicy(resourceIdentifier, config.getConfig());
        policy = policy == null
                ? getConflictPolicy(resourceIdentifier, config.getDefaultConfig())
                : policy;
        return policy;
    }

    private JsonMergeConflictPolicy getConflictPolicy(ResourceIdentifier resourceIdentifier, Map<PathMatcher, JsonMergeConflictPolicy> conf) {
        Path path;
        String resourcePath = resourceIdentifier.getFilePath();
        try {
            path = Path.of(resourcePath);
        } catch (InvalidPathException e) {
            printStackTrace(e);
            throw new RuntimeException("Not a path [%s]".formatted(resourcePath));
        }

        Set<String> matches = new HashSet<>();
        JsonMergeConflictPolicy policy = null;
        for (Entry<PathMatcher, JsonMergeConflictPolicy> e : conf.entrySet()) {
            PathMatcher globMatcher = e.getKey();
            if (globMatcher.matches(path)) {
                matches.add(e.getValue().name());
                policy = e.getValue();
            }
        }

        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple pattern matches for [" + resourcePath + "]: " + matches);
        }

        return policy;
    }

    private void printStackTrace(Throwable throwable) {
        Messager messager = processingEnv.getMessager();
        try (
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter)
        ) {
            throwable.printStackTrace(printWriter);
            messager.printNote(stringWriter.getBuffer());
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(
                AnnotationProcessorConstants.RESOURCE_LOCATIONS_OPTION,
                AnnotationProcessorConstants.CONFIG_OPTION
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME,
                ModPackage.class.getName()
        );
    }
}