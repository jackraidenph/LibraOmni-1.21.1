package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.reflect.context.ModContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public interface RuntimeTask {

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
