package dev.jackraidenph.libraomni.annotation.compile.impl;

import dev.jackraidenph.libraomni.annotation.compile.api.CompileTimeProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class AbstractCompileTimeProcessor implements CompileTimeProcessor {

    private final ProcessingEnvironment processingEnvironment;

    public AbstractCompileTimeProcessor(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        return true;
    }

    @Override
    public boolean onFinish(RoundEnvironment roundEnvironment) {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of();
    }

    @Override
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnvironment;
    }
}
