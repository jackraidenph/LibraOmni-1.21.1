package dev.jackraidenph.libraomni.data.proxy.runtime;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.data.proxy.AbstractObjectProxy;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class AnnotatedElementProxy extends AbstractObjectProxy<AnnotatedElement> implements ProxiedAnnotatedElement {

    private final AnnotatedElementCache cache;

    public AnnotatedElementProxy(AnnotatedElement proxiedObject) {
        super(proxiedObject);
        this.cache = new AnnotatedElementCache(proxiedObject);
    }

    @Override
    @InterceptorFor("proxiedElement")
    public AnnotatedElement proxiedElement() {
        return proxiedObject;
    }

    @Override
    @InterceptorFor("getAnnotation")
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getAnnotation(annotationClass, false);
    }

    @Override
    @InterceptorFor("getDeclaredAnnotation")
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        return cache.getAnnotation(annotationClass, true);
    }

    @Override
    @InterceptorFor("getAnnotations")
    public Annotation[] getAnnotations() {
        return cache.getAnnotations(false);
    }

    @Override
    @InterceptorFor("getDeclaredAnnotations")
    public Annotation[] getDeclaredAnnotations() {
        return cache.getAnnotations(true);
    }

    @Override
    @InterceptorFor("isAnnotationPresent")
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return cache.hasAnnotation(annotationClass);
    }

    @Override
    @InterceptorFor("getAnnotationsByType")
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return cache.getAnnotationsByType(annotationClass, false);
    }

    @Override
    @InterceptorFor("getDeclaredAnnotationsByType")
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return cache.getAnnotationsByType(annotationClass, true);
    }
}
