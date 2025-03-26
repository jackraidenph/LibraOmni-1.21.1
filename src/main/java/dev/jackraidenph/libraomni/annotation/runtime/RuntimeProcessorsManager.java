package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.data.ElementData;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.util.context.ModContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeProcessorsManager {

    private final Map<String, ElementData> elementDataMap = new HashMap<>();

    private final Map<Scope, List<RuntimeProcessor>> processors = new HashMap<>();

    private final Set<ModContext> modsToProcess = new HashSet<>();

    private static RuntimeProcessorsManager INSTANCE;

    public static RuntimeProcessorsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuntimeProcessorsManager();
        }

        return INSTANCE;
    }

    private RuntimeProcessorsManager() {
    }

    private Set<AnnotatedElement> readElements(String modId) {
        if (elementDataMap.containsKey(modId)) {
            return elementDataMap.get(modId).getElements();
        }

        ElementData elementData = MetadataFileManager.reader().readElementData(modId);
        if (elementData != null) {
            this.elementDataMap.put(modId, elementData);
            return elementData.getElements();
        }

        return Set.of();
    }

    private static boolean anyAnnotationPresent(AnnotatedElement e, Set<Class<? extends Annotation>> annotations) {
        for (Class<? extends Annotation> a : annotations) {
            if (e.isAnnotationPresent(a)) {
                return true;
            }
        }

        return false;
    }

    private Set<AnnotatedElement> elementsAnnotatedWith(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }
        return this.readElements(modId).stream()
                .filter(e -> anyAnnotationPresent(e, annotations))
                .collect(Collectors.toSet());
    }

    public void registerMod(ModContext modContext) {
        this.modsToProcess.add(modContext);
    }

    private Set<RuntimeProcessor> allProcessors() {
        return this.processors.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public void registerProcessor(Scope scope, RuntimeProcessor runTimeProcessor) {
        if (this.allProcessors().contains(runTimeProcessor)) {
            throw new IllegalArgumentException("Runtime processor already registered");
        }
        this.processors.computeIfAbsent(scope, s -> new ArrayList<>()).add(runTimeProcessor);
    }

    public void processAll(Scope scope) {
        List<RuntimeProcessor> processors = this.processors.get(scope);
        if (processors == null || processors.isEmpty()) {
            return;
        }

        for (ModContext modContext : this.modsToProcess) {
            for (RuntimeProcessor runtimeProcessor : processors) {
                Set<AnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeProcessor.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info(
                        "Processing mod id [{}] with [{}] in {}",
                        modContext.modId(),
                        runtimeProcessor.getClass().getSimpleName(),
                        scope
                );

                runtimeProcessor.process(modContext, elements);
            }
        }
    }
}
