package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;

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

    private void addAnnotation(Map<String, Entry<Delegate, Object>> delegates, Annotation annotation) {
        boolean delegated = delegates != null && !delegates.isEmpty();
        Annotation proxyOrSelf = delegated ? ProxyFactory.proxifyAnnotation(annotation, delegates) : annotation;
        annotationMap.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>()).add(proxyOrSelf);
    }

    private void cacheStep(AnnotatedConstruct construct, Map<String, Entry<Delegate, Object>> delegates, AnnotationMirror current) {
        TypeElement currentElement = addAnnotationMirror(current);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(currentElement.getQualifiedName().toString(), Annotation.class);
        if (clazz != null) {
            Annotation annotation = construct.getAnnotation(clazz);
            addAnnotation(delegates, annotation);
        }

        Composed composed = currentElement.getAnnotation(Composed.class);
        if (composed == null) {
            return;
        }

        for (AnnotationMirror child : currentElement.getAnnotationMirrors()) {
            TypeElement childElement = (TypeElement) child.getAnnotationType().asElement();
            String childName = childElement.getQualifiedName().toString();
            Map<String, Entry<Delegate, Object>> map = ProxyFactory.mapDelegatesFromAnnotationMirror(childName, current, delegates);
            cacheStep(currentElement, map, child);
        }
    }
}
