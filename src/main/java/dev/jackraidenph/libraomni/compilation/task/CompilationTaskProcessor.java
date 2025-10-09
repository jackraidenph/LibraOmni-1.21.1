package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.service.ModPackage;
import dev.jackraidenph.libraomni.compilation.util.AnnotationProcessorConfig;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.Resource;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private final Set<CompilationTask> tasks = new HashSet<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final AnnotationProcessorConfig config = new AnnotationProcessorConfig();
    private final Map<String, URI> generatedResources = new HashMap<>();

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

        Set<Resource> createdResources = new HashSet<>();

        for (CompilationTask compilationTask : this.tasks) {
            final String op = finishing ? "Finishing" : "Processing";

            messager.printNote(op + " [" + compilationTask.getClass().getSimpleName() + "]");

            try {
                RoundEnvironment proxyEnvironment = ProxyFactory.proxifyRuntimeEnvironment(roundEnvironment, processingEnv);
                Collection<Resource> output = !finishing
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

        saveAllResourcesToDisk(createdResources);

        this.round++;
        return false;
    }

    private void saveAllResourcesToDisk(Collection<Resource> resources) {
        for (Resource resource : resources) {
            Resource mergeResult = resolveConflictIfPresent(resource);
            if (mergeResult == null) {
                continue;
            }
            FileObject created = mergeResult.getFileObject(processingEnv.getFiler());//mergeResult.saveToClassOutput(processingEnv.getFiler());
            mergeResult.saveToUri(created.toUri());
            generatedResources.put(mergeResult.getFilePath(), created.toUri());
        }
    }

    private JsonMergeConflictPolicy getConflictPolicy(Resource resource) {
        JsonMergeConflictPolicy policy = getConflictPolicy(resource, config.getConfig());
        if (policy == null) {
            policy = getConflictPolicy(resource, config.getDefaultConfig());
        }
        if (policy == null) {
            policy = JsonMergeConflictPolicy.OVERWRITE;
            processingEnv.getMessager().printWarning("Failed to get conflict resolution policy for [%s], assuming [%s]".formatted(resource, policy));
        }
        return policy;
    }

    private JsonMergeConflictPolicy getConflictPolicy(Resource resource, Map<PathMatcher, JsonMergeConflictPolicy> conf) {
        Path path;
        String resourcePath = resource.getFilePath();
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

    private Optional<Resource> getPreviouslyGenerated(String dir, String nameRoot, String extension) {
        String path = dir + nameRoot + '.' + extension;
        try {
            URI uri = generatedResources.get(path);

            try (InputStream inputStream = new FileInputStream(uri.getPath())) {
                return Optional.of(
                        Resource.builder()
                                .setDirectory(dir)
                                .setNameRoot(nameRoot)
                                .setExtension(extension)
                                .setRawBytes(inputStream.readAllBytes())
                                .build()
                );
            }

        } catch (IOException ioException) {
            throw new RuntimeException("Resource [" + path + "] found, but can't be opened", ioException);
        }
    }

    private Resource resolveConflictIfPresent(Resource toSave) {
        Messager messager = processingEnv.getMessager();

        Optional<Resource> existing = Optional.empty();
        if (generatedResources.containsKey(toSave.getFilePath())) {
            existing = getPreviouslyGenerated(toSave.getDirectory(), toSave.getNameRoot(), toSave.getExtension());
        }

        if (existing.isEmpty() && toSave.resourceExistsOnDisk(config.getResourceSetDirs())) {
            existing = Resource.builder().copyFilePathFrom(toSave).tryRead(config.getResourceSetDirs());
        }

        if (existing.isEmpty()) {
            return toSave;
        }

        String ext = toSave.getExtension();
        if (ext.equals(Resource.JSON_EXT)) {
            JsonMergeConflictPolicy conflictPolicy = getConflictPolicy(toSave);
            messager.printNote("Resource [%s] already exists, trying to merge with policy [%s]".formatted(toSave, conflictPolicy));
            return JsonMergeHelper.mergeJson(existing.get(), toSave, conflictPolicy);
        } else {
            messager.printNote("Resources [%s] already exists, but no merge methods are known for [%s] extension, skipping".formatted(toSave, ext));
            return null;
        }
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