package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.*;

public abstract class AnnotationMirrorCachingInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedConstruct> {

    protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();

    public AnnotationMirrorCachingInvocationHandler(AnnotatedConstruct original) {
        super(original);
        cacheRecursive(original);
    }

    private void cacheRecursive(AnnotatedConstruct original) {
        for (AnnotationMirror annotation : original.getAnnotationMirrors()) {
            cacheStep(original, null, annotation);
        }
    }

    private TypeElement addAnnotationMirror(AnnotationMirror mirror) {
        TypeElement typeElement = (TypeElement) mirror.getAnnotationType().asElement();
        if (!(typeElement.getQualifiedName().contentEquals(Composed.class.getName()))) {
            annotationMirrorsMap.computeIfAbsent(typeElement, k -> new ArrayList<>()).add(mirror);
        }
        return typeElement;
    }

    //FIX Delegation at compile-time
    private void addAnnotation(Class<? extends Annotation> type, Annotation parent, Annotation annotation) {
        if (!(annotation instanceof Composed)) {
            Annotation proxyOrSelf = parent == null ? annotation : ProxyFactory.proxifyAnnotation(annotation, parent);
            annotationMap.computeIfAbsent(type, k -> new ArrayList<>()).add(proxyOrSelf);
        }
    }

    private void cacheStep(AnnotatedConstruct construct, Annotation parentOrNull, AnnotationMirror parentAnnotation) {
        TypeElement typeElement = addAnnotationMirror(parentAnnotation);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(typeElement.getQualifiedName().toString(), Annotation.class);
        Annotation annotation = null;
        if (clazz != null) {
            annotation = construct.getAnnotation(clazz);
            addAnnotation(clazz, parentOrNull, annotation);
        }

        Composed composed = typeElement.getAnnotation(Composed.class);
        if (composed == null) {
            return;
        }

        for (AnnotationMirror metaAnnotation : typeElement.getAnnotationMirrors()) {
            cacheStep(typeElement, annotation, metaAnnotation);
        }
    }
}
