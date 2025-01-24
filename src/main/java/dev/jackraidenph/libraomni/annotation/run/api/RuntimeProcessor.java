package dev.jackraidenph.libraomni.annotation.run.api;

import dev.jackraidenph.libraomni.annotation.run.util.ReferenceMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface RuntimeProcessor {

    void process(
            ModContext modContext,
            Scope scope,
            Class<? extends Annotation> annotation,
            AnnotatedElement<?> annotatedElement
    );

    default void onFinish(ModContext modContext) {

    }

    Set<Class<? extends Annotation>> getSupportedAnnotations();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
