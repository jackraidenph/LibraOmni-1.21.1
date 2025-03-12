package dev.jackraidenph.libraomni.annotation.run;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.impl.resource.AnnotationMapProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.util.ModContext;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader;
import dev.jackraidenph.libraomni.annotation.run.util.ElementStorage;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeProcessorsManager {

    private final Map<String, ElementStorage> elementStoragePerMod = new HashMap<>();

    private final Map<Scope, List<RuntimeProcessor>> processors = new HashMap<>();

    private final Set<ModContext> modsToProcess = new HashSet<>();

    public RuntimeProcessorsManager() {
    }

    private Set<AnnotatedElement> gatherElements(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }

        ElementStorage elementStorage = this.elementStoragePerMod.get(modId);

        if (elementStorage == null) {
            return Set.of();
        }

        Set<AnnotatedElement> elements = new HashSet<>();
        for (Class<? extends Annotation> annotation : annotations) {
            elements.addAll(elementStorage.elementsAnnotatedWith(annotation));
        }

        return elements;
    }

    public void prepareForMod(ModContext modContext) {
        this.modsToProcess.add(modContext);

        String modId = modContext.modId();

        ElementStorage elementStorage = new ElementStorage();

        this.elementStoragePerMod.put(modId, elementStorage);

        try {
            AnnotationMapReader.readElementsToStorage(
                    AnnotationMapProcessor.annotationsFileLocation(modId),
                    elementStorage
            );
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException("An exception was thrown while trying to read annotation map", e);
        }
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
                Set<AnnotatedElement> elements = this.gatherElements(
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
