package dev.jackraidenph.libraomni.data.proxy.runtime;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class AnnotatedElementWrapper implements ProxiedAnnotatedElement {

    private final AnnotatedElementCache cache;

    public AnnotatedElementWrapper(AnnotatedElement proxiedObject) {
        this.cache = new AnnotatedElementCache(proxiedObject);
    }

    @Override
    public AnnotatedElement proxiedElement() {
        return cache.annotatedElement;
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getAnnotation(annotationClass, false);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getAnnotation(annotationClass, true);
    }

    @Override
    public Annotation[] getAnnotations() {
        return cache.getAnnotations(false);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return cache.getAnnotations(true);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return cache.hasAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return cache.getAnnotationsByType(annotationClass, false);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return cache.getAnnotationsByType(annotationClass, true);
    }

    @Override
    public String toString() {
        return "Wrapped@" + cache.annotatedElement;
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return cache.annotatedElement.equals(obj);
    }

    @Override
    public int hashCode() {
        return cache.annotatedElement.hashCode();
    }
}
