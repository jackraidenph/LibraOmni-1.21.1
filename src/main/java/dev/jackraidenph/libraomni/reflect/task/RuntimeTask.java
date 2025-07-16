package dev.jackraidenph.libraomni.reflect.task;

import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.reflect.ModContext;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface RuntimeTask {

    void process(
            ModContext modContext,
            Set<TransitiveAnnotatedElement> elements
    );

    LifecycleStage getExecutionStage();

    default Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of();
    }

    default Set<Class<? extends RuntimeTask>> dependsOn() {
        return Set.of();
    }
}
