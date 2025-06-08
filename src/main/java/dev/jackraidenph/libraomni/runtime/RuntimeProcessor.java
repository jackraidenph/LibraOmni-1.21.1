package dev.jackraidenph.libraomni.runtime;

import dev.jackraidenph.libraomni.runtime.context.ModContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public interface RuntimeProcessor {

    void process(
            ModContext modContext,
            Set<AnnotatedElement> elements
    );

    Set<Class<? extends Annotation>> getSupportedAnnotations();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
