package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.data.proxy.AnnotationAccessor;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public interface RuntimeTask {

    void process(
            ModContext modContext,
            Set<AnnotationAccessor<AnnotatedElement>> elements
    );

    LifecycleStage getExecutionStage();

    default Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of();
    }

    default Set<Class<? extends RuntimeTask>> dependsOn() {
        return Set.of();
    }
}
