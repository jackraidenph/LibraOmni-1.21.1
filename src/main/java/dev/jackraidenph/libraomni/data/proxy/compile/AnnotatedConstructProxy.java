package dev.jackraidenph.libraomni.data.proxy.compile;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.data.proxy.AbstractObjectProxy;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotatedConstructProxy extends AbstractObjectProxy<AnnotatedConstruct> implements AnnotatedConstruct {

    private final AnnotatedConstructCache cache;

    public AnnotatedConstructProxy(AnnotatedConstruct proxiedObject) {
        super(proxiedObject);
        this.cache = new AnnotatedConstructCache(proxiedObject);
    }

    @Override
    @InterceptorFor("getAnnotationsByType")
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> clazz) {
        return cache.getAnnotationsByType(clazz);
    }

    @Override
    @InterceptorFor("getAnnotation")
    public <A extends Annotation> A getAnnotation(Class<A> clazz) {
        return cache.getAnnotation(clazz);
    }

    @Override
    @InterceptorFor("getAnnotationMirrors")
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return cache.getAnnotationMirrors();
    }

    @Override
    @InterceptorFor("hashCode")
    public int hashCode() {
        return this.proxiedObject.hashCode();
    }

    @Override
    @InterceptorFor("equals")
    public boolean equals(Object object) {
        return object instanceof AnnotatedConstruct && object.hashCode() == this.hashCode();
    }
}
