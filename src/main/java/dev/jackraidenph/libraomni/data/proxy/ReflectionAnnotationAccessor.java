package dev.jackraidenph.libraomni.data.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;

public class ReflectionAnnotationAccessor implements AnnotationAccessor<AnnotatedElement> {

    private final AnnotatedElement element;

    public ReflectionAnnotationAccessor(AnnotatedElement element) {
        this.element = element;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return Arrays.asList(element.getDeclaredAnnotations());
    }

    @Override
    public AnnotatedElement unwrap() {
        return this.element;
    }
}
