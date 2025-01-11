package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.management;

import dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.stereotype.RuntimeAnnotationProcessor;

import java.util.*;

public class RuntimeAnnotationProcessorsManager {

    private final List<RuntimeAnnotationProcessor<?>> processors;
    private final Map<Class<?>, List<Class<?>>> annotationProcessorListMap;
    private final Set<Class<?>> registeredProcessors;

    private final ProcessableElementsStorage processableElementsStorage;

    public RuntimeAnnotationProcessorsManager(ProcessableElementsStorage elementsStorage) {
        this.processors = new ArrayList<>();
        this.annotationProcessorListMap = new HashMap<>();
        this.registeredProcessors = new HashSet<>();

        this.processableElementsStorage = elementsStorage;
    }

    public void addProcessor(Class<?> annotation, Class<?> processor) {
        if (this.registeredProcessors.contains(processor)) {
            throw new IllegalArgumentException("Processor already registered");
        }

        this.annotationProcessorListMap.computeIfAbsent(annotation, k -> new ArrayList<>()).add(processor);
        this.registeredProcessors.add(processor);
    }

    public void removeProcessor(Class<?> annotation, Class<?> processor) {
        List<Class<?>> processors = this.annotationProcessorListMap.get(annotation);
        processors.remove(processor);
    }


    public void processAll() {
        for (RuntimeAnnotationProcessor<?> annotationProcessor : this.processors) {
            //annotationProcessor.process();
        }
    }

}
