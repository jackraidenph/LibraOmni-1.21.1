package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.lang.annotation.Annotation;
import java.util.Set;

interface CompilationTask {

    void processRound(ModIdGetter modLocator, ProcessingContext processingContext);

    default void finish(ModIdGetter modLocator, ProcessingContext processingContext) {

    }

    //Every captured annotation is processed if empty
    default Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }

    default boolean isAnnotation(Element e) {
        return e.getKind().equals(ElementKind.ANNOTATION_TYPE);
    }
}
