package dev.jackraidenph.libraomni.annotation.run.api;

import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;

public interface RuntimeProcessor<T extends Annotation> {

    void process(
            ModContext modContext,
            AnnotatedElement<?> annotatedElement
    );

    @Nullable
    default T getElementAnnotation(AnnotatedElement<?> element) {
        return element.getAnnotation(this.getSupportedAnnotation());
    }

    Class<T> getSupportedAnnotation();

    Scope getScope();

    enum Scope {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}
