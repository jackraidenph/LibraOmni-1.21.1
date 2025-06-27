package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface RuntimeTask {

    void process(
            ModContext modContext,
            Set<TransitiveAnnotatedElement> elements
    );

    Set<Class<? extends Annotation>> getSupportedAnnotations();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
