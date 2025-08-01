package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public abstract class AnnotationCachingInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedElement> implements AnnotationAccessor<AnnotatedElement> {

    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();

    public AnnotationCachingInvocationHandler(AnnotatedElement original) {
        super(original);
        cacheRecursive(original);
    }

    @Override
    public AnnotatedElement annotatedObject() {
        return original;
    }

    @Override
    public Collection<Annotation> getAllAnnotations() {
        return annotationMap.values().stream().flatMap(List::stream).toList();
    }

    protected Annotation[] getProxiedRecursiveAnnotations() {
        return getAllAnnotations().toArray(Annotation[]::new);
    }

    private void cacheRecursive(AnnotatedElement accessor) {
        for (Annotation annotation : accessor.getDeclaredAnnotations()) {
            cacheStep(annotation);
        }
    }

    private void addAnnotation(Annotation annotation) {
        if (!(annotation instanceof Composed)) {
            Class<? extends Annotation> type = annotation.annotationType();
            annotationMap.computeIfAbsent(type, k -> new ArrayList<>()).add(annotation);
        }
    }

    private void cacheStep(Annotation parentAnnotation) {
        addAnnotation(parentAnnotation);

        Class<? extends Annotation> type = parentAnnotation.annotationType();
        Composed composed = type.getAnnotation(Composed.class);

        if (composed == null) {
            return;
        }

        for (Annotation metaAnnotation : type.getAnnotations()) {
            Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
            cacheStep(metaProxy);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAllAnnotations" -> getAllAnnotations();
            case "getAnnotationByClass" -> getAnnotationByClass((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByClass" -> getAnnotationsByClass((Class<? extends Annotation>) args[0]);
            case "isAnnotationPresent" -> isAnnotationPresent((Class<? extends Annotation>) args[0]);
            case "annotatedObject" -> annotatedObject();
            default -> super.invoke(proxy, method, args);
        };

    }
}
