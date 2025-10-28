package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.lang.annotation.Annotation;
import java.util.Set;

interface CompilationTask {

    void processRound(ModIdGetter modLocator, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);

    default void finish(ModIdGetter modLocator, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {

    }

    //Every captured annotation is processed if empty
    default Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }

    default boolean isAnnotation(Element e) {
        return e.getKind().equals(ElementKind.ANNOTATION_TYPE);
    }
}
