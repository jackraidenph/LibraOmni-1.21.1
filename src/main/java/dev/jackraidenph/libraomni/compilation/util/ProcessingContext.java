package dev.jackraidenph.libraomni.compilation.util;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

public class ProcessingContext {

    private ModIdGetter modIdGetter;
    private ResourceManager resourceManager;
    private AnnotationProcessorConfig config;
    private RoundEnvironment roundEnvironment;
    private ProcessingEnvironment processingEnvironment;
    private final int round;

    public ProcessingContext(
            @Nullable ModIdGetter modIdGetter,
            @Nullable ResourceManager resourceManager,
            @Nullable AnnotationProcessorConfig config,
            @Nullable RoundEnvironment roundEnvironment,
            @Nullable ProcessingEnvironment processingEnvironment,
            int round
    ) {
        this.modIdGetter = modIdGetter;
        this.resourceManager = resourceManager;
        this.config = config;
        this.roundEnvironment = roundEnvironment;
        this.processingEnvironment = processingEnvironment;
        this.round = round;
    }

    public ModIdGetter modIdGetter() {
        return modIdGetter;
    }

    public void setModIdGetter(ModIdGetter modIdGetter) {
        this.modIdGetter = modIdGetter;
    }

    public ResourceManager resourceManager() {
        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public AnnotationProcessorConfig config() {
        return config;
    }

    public void setConfig(AnnotationProcessorConfig config) {
        this.config = config;
    }

    public RoundEnvironment roundEnvironment() {
        return roundEnvironment;
    }

    public void setRoundEnvironment(RoundEnvironment roundEnvironment) {
        this.roundEnvironment = roundEnvironment;
    }

    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    public void setProcessingEnvironment(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    public int round() {
        return round;
    }
}
