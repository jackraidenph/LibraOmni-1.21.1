package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.data.cache.AnnotationCache;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public class AnnotatedElementProxy extends AbstractObjectProxy<AnnotatedElement> {

    private final AnnotationCache cache;
    private ModMetadataReader modMetadataReader;

    public AnnotatedElementProxy(AnnotatedElement proxiedObject, ModMetadataReader modMetadataReader) {
        super(proxiedObject);
        this.cache = new AnnotationCache(proxiedObject);
    }

    @InterceptorFor("proxiedElement")
    private AnnotatedElement proxiedElement() {
        return proxiedObject;
    }

    @InterceptorFor("getAnnotation")
    private <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getAnnotation(annotationClass);
    }

    @InterceptorFor("getDeclaredAnnotation")
    private <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getDeclaredAnnotation(annotationClass);
    }

    @InterceptorFor("getAnnotations")
    private Annotation[] getAnnotations() {
        return cache.getAnnotations();
    }

    @InterceptorFor("getDeclaredAnnotations")
    private Annotation[] getDeclaredAnnotations() {
        return cache.getDeclaredAnnotations();
    }

    @InterceptorFor("isAnnotationPresent")
    private boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return cache.isAnnotationPresent(annotationClass);
    }

    @InterceptorFor("getAnnotationsByType")
    private <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return cache.getAnnotationsByType(annotationClass);
    }

    @InterceptorFor("getDeclaredAnnotationsByType")
    private <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return cache.getDeclaredAnnotationsByType(annotationClass);
    }
}
