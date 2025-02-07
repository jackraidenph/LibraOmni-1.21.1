package dev.jackraidenph.libraomni.annotation.run.api;

import dev.jackraidenph.libraomni.annotation.run.util.ClassMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;

import java.lang.annotation.Annotation;

public interface RuntimeProcessor {

    void process(
            ModContext modContext,
            Class<? extends Annotation> annotation,
            AnnotatedElement<?> annotatedElement
    );

    Class<? extends Annotation> getSupportedAnnotation();

    Scope getScope();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
