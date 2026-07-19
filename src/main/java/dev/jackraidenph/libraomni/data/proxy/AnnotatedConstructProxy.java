package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.data.cache.AnnotationMirrorCache;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotatedConstructProxy extends AbstractObjectProxy<AnnotatedConstruct> {

    private final AnnotationMirrorCache cache;
    private final ModIdGetter modIdGetter;

    public AnnotatedConstructProxy(AnnotatedConstruct proxiedObject, ModIdGetter modIdGetter) {
        super(proxiedObject);
        this.modIdGetter = modIdGetter;
        this.cache = new AnnotationMirrorCache(proxiedObject);
    }

    @InterceptorFor("getAnnotationsByType")
    private <A extends Annotation> A[] getAnnotationsByType(Class<A> clazz) {
        return cache.getAnnotationsByType(clazz);
    }

    @InterceptorFor("getAnnotation")
    private <A extends Annotation> A getAnnotation(Class<A> clazz) {
        return cache.getAnnotation(clazz);
    }

    @InterceptorFor("getAnnotationMirrors")
    private List<? extends AnnotationMirror> getAnnotationMirrors() {
        return cache.getAnnotationMirrors();
    }
}
