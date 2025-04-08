package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.Set;

abstract class AbstractCompilationProcessor implements CompilationProcessor {

    private final ProcessingEnvironment processingEnvironment;

    protected AbstractCompilationProcessor(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    @Override
    public void processRound(RoundEnvironment roundEnvironment) {
    }

    @Override
    public void finish(RoundEnvironment roundEnvironment) {
    }

    @Override
    public ProcessingEnvironment processingEnvironment() {
        return this.processingEnvironment;
    }

    protected Messager messager() {
        return this.processingEnvironment().getMessager();
    }

    protected Filer filer() {
        return this.processingEnvironment().getFiler();
    }

    protected Elements elementUtils() {
        return this.processingEnvironment().getElementUtils();
    }

    protected Types typeUtils() {
        return this.processingEnvironment().getTypeUtils();
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }
}
