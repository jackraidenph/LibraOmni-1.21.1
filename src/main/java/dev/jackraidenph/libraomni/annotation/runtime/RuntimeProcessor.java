package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.util.context.ModContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public interface RuntimeProcessor {

    void process(
            ModContext modContext,
            Set<AnnotatedElement> elements
    );

    Set<Class<? extends Annotation>> getSupportedAnnotations();

    Scope getScope();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
