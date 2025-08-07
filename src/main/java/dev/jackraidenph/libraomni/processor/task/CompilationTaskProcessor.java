package dev.jackraidenph.libraomni.processor.task;

import dev.jackraidenph.libraomni.common.CommonGson;
import dev.jackraidenph.libraomni.data.ProjectMetadata;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.processor.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.processor.util.JsonMergeHelper;
import dev.jackraidenph.libraomni.processor.util.JsonMergeHelper.JsonMergeConflictPolicy;
import dev.jackraidenph.libraomni.processor.util.ModIdGetter;
import dev.jackraidenph.libraomni.processor.util.Resource;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private final Set<CompilationTask> tasks = new HashSet<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final Set<String> resourceDirs = new HashSet<>();
    private final Map<Pattern, JsonMergeConflictPolicy> defaultConfig = Map.of(
            Pattern.compile("data/.*"), JsonMergeConflictPolicy.PREFER_NEW,
            Pattern.compile("assets/.*"), JsonMergeConflictPolicy.OVERWRITE
    );
    private final Map<Pattern, JsonMergeConflictPolicy> config = new HashMap<>();
    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        gatherResourceDirs();
        gatherConfig();
        RegisteredCompilationTask.init(this);
    }

    private void gatherResourceDirs() {
        String resourcesPaths = processingEnv.getOptions().get(AnnotationProcessorConstants.RESOURCE_LOCATIONS_OPTION);
        if (resourcesPaths != null) {
            resourceDirs.addAll(Arrays.asList(resourcesPaths.split(";")));
        }
    }

    private void gatherConfig() {
        String configLoc = processingEnv.getOptions().get(AnnotationProcessorConstants.CONFIG_LOCATION_OPTION);
        if (configLoc != null) {
            processingEnv.getMessager().printNote("Got Annotation Processor config location [%s]".formatted(configLoc));
        } else {
            configLoc = ProjectMetadata.DIRECTORY;
            processingEnv.getMessager().printNote("Annotation Processor config location not specified, assuming [%s]".formatted(configLoc));
        }
        Optional<Resource> configResource = Resource.builder()
                .setDirectory(configLoc)
                .setNameRoot(AnnotationProcessorConstants.CONFIG_NAME)
                .setJsonExtension()
                .tryRead(resourceDirs);

        if (configResource.isPresent()) {
            String configStr = new String(configResource.get().getContents(), StandardCharsets.UTF_8);
            //noinspection unchecked
            CommonGson.DEFAULT.fromJson(configStr, Map.class).forEach((path, policy) ->
                    config.put(Pattern.compile((String) path), JsonMergeConflictPolicy.valueOf((String) policy))
            );
            processingEnv.getMessager().printNote("Annotation Processor config found and processed: %s, backup values: %s".formatted(config, defaultConfig));
        } else {
            processingEnv.getMessager().printNote("Annotation Processor config not found, assuming default values %s".formatted(defaultConfig));
        }
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

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME);
        this.modIdGetter.findMods(modAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());

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
            mergeResult.saveToClassOutput(processingEnv.getFiler());
        }
    }

    private JsonMergeConflictPolicy getConflictPolicy(Resource resource) {
        JsonMergeConflictPolicy policy = getConflictPolicy(resource, config);
        if (policy == null) {
            policy = getConflictPolicy(resource, defaultConfig);
        }
        if (policy == null) {
            policy = JsonMergeConflictPolicy.OVERWRITE;
            processingEnv.getMessager().printWarning("Failed to get conflict resolution policy for [%s], assuming [%s]".formatted(resource, policy));
        }
        return policy;
    }

    private static JsonMergeConflictPolicy getConflictPolicy(Resource resource, Map<Pattern, JsonMergeConflictPolicy> conf) {
        String path = resource.getFilePath();
        for (Entry<Pattern, JsonMergeConflictPolicy> e : conf.entrySet()) {
            Pattern regex = e.getKey();
            if (regex.matcher(path).matches()) {
                return e.getValue();
            }
        }

        return null;
    }

    private Resource resolveConflictIfPresent(Resource toSave) {
        Messager messager = processingEnv.getMessager();

        if (!toSave.resourceExistsOnDisk(resourceDirs)) {
            return toSave;
        }

        Optional<Resource> existing = Resource.builder().copyFilePathFrom(toSave).tryRead(resourceDirs);
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
                AnnotationProcessorConstants.CONFIG_LOCATION_OPTION
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME);
    }
}