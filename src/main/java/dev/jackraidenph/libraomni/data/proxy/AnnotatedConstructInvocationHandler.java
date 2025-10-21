package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedConstructInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedConstruct> {

    protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();
    private final Elements elementUtils;

    public AnnotatedConstructInvocationHandler(AnnotatedConstruct original, Elements elements) {
        super(original);
        elementUtils = elements;
        cacheRecursive(original);
    }

    private void cacheRecursive(AnnotatedConstruct original) {
        for (AnnotationMirror annotation : elementUtils.getAllAnnotationMirrors((Element) original)) {
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

    private void addAnnotation(DelegateContainer delegates, Annotation annotation) {
        if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
            return;
        }

        boolean delegated = delegates != null && !delegates.isEmpty();
        Annotation proxyOrSelf = delegated ? ProxyFactory.proxifyAnnotation(annotation, delegates) : annotation;
        annotationMap.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>()).add(proxyOrSelf);
    }

    private void cacheStep(AnnotatedConstruct construct, DelegateContainer delegates, AnnotationMirror current) {
        TypeElement currentElement = addAnnotationMirror(current);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(elementUtils.getBinaryName(currentElement).toString(), Annotation.class);
        if (clazz != null) {
            Annotation annotation = construct.getAnnotation(clazz);
            addAnnotation(delegates, annotation);
        }

        Composed composed = currentElement.getAnnotation(Composed.class);
        if (composed == null) {
            return;
        }

        for (AnnotationMirror child : elementUtils.getAllAnnotationMirrors(currentElement)) {
            //Prevent self-recursion
            if (child.getAnnotationType().equals(current.getAnnotationType())) {
                continue;
            }
            TypeElement childElement = (TypeElement) child.getAnnotationType().asElement();
            String childName = childElement.getQualifiedName().toString();
            DelegateContainer container = ProxyFactory.mapDelegatesFromAnnotationMirror(childName, current, delegates);
            cacheStep(currentElement, container, child);
        }
    }

    private <A extends Annotation> A[] byType(Class<A> clazz) {
        if (ProxyFactory.ONLY_DIRECT.contains(clazz)) {
            //noinspection unchecked
            A[] arr = (A[]) Array.newInstance(clazz, 1);
            arr[0] = original.getAnnotation(clazz);
            return arr;
        }
        List<Annotation> annotations = annotationMap.get(clazz);
        if (annotations != null && !annotations.isEmpty()) {
            //noinspection unchecked
            return (A[]) annotations.toArray(Annotation[]::new);
        }
        //noinspection unchecked
        return (A[]) Array.newInstance(clazz, 0);
    }

    private <A extends Annotation> A byTypeSingular(Class<A> clazz) {
        A[] arr = byType(clazz);
        if (arr.length == 0) {
            return null;
        }
        return arr[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAnnotationMirrors" -> annotationMirrorsMap.values().stream().flatMap(List::stream).toList();
            case "getAnnotation" -> byTypeSingular((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByType" -> byType((Class<? extends Annotation>) args[0]);
            case null -> throw new IllegalStateException();
            default -> super.invoke(proxy, method, args);
        };
    }
}
