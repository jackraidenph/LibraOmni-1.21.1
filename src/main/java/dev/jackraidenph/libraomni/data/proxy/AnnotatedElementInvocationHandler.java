package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedElementInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedElement> implements ProxyAnnotatedElement {

    protected final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

    public AnnotatedElementInvocationHandler(AnnotatedElement original) {
        super(original);
        cacheRecursive(original.getAnnotations(), annotations, false);
        cacheRecursive(original.getDeclaredAnnotations(), declaredAnnotations, true);
    }

    @Override
    public AnnotatedElement original() {
        return original;
    }

    public <T extends Annotation> T getAnnotationFrom(@NotNull Class<T> annotationClass, Map<Class<? extends Annotation>, List<Annotation>> map) {
        List<Annotation> byClass = map.get(annotationClass);
        //If no annotations of the type found
        if (byClass == null || byClass.isEmpty()) {
            return null;
        }
        return annotationClass.cast(byClass.getFirst());
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getAnnotation(annotationClass);
        }
        return getAnnotationFrom(annotationClass, annotations);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getDeclaredAnnotation(annotationClass);
        }
        return getAnnotationFrom(annotationClass, declaredAnnotations);
    }

    @Override
    public @NotNull Annotation[] getAnnotations() {
        return annotations.values().stream().flatMap(List::stream).toArray(Annotation[]::new);
    }

    @Override
    public @NotNull Annotation[] getDeclaredAnnotations() {
        return declaredAnnotations.values().stream().flatMap(List::stream).toArray(Annotation[]::new);
    }

    private void cacheRecursive(Annotation[] rootAnnotations, Map<Class<? extends Annotation>, List<Annotation>> addTo, boolean declared) {
        for (Annotation annotation : rootAnnotations) {
            cacheStep(annotation, addTo, declared);
        }
    }

    private void addAnnotation(Annotation annotation, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
        if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
            return;
        }

        List<Annotation> annotations = addTo.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>());
        if (!annotations.isEmpty()) {
            boolean repeatable = annotation.annotationType().getAnnotation(Repeatable.class) != null;
            if (!repeatable) {
                throw new IllegalStateException("Annotation [%s] is not repeatable, previous encounter: [%s]".formatted(annotation, annotations.getFirst()));
            }
        }
        annotations.add(annotation);
    }

    private void cacheStep(Annotation parentAnnotation, Map<Class<? extends Annotation>, List<Annotation>> addTo, boolean declared) {
        addAnnotation(parentAnnotation, addTo);

        Class<? extends Annotation> type = parentAnnotation.annotationType();
        Composed composed = type.getAnnotation(Composed.class);

        if (composed == null) {
            return;
        }

        Annotation[] metaAnnotations = declared ? type.getDeclaredAnnotations() : type.getAnnotations();
        for (Annotation metaAnnotation : metaAnnotations) {
            Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
            cacheStep(metaProxy, addTo, declared);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "isAnnotationPresent" -> isAnnotationPresent((Class<? extends Annotation>) args[0]);
            case "getAnnotation" -> getAnnotation((Class<? extends Annotation>) args[0]);
            case "getAnnotations" -> getAnnotations();
            case "getAnnotationsByType" -> getAnnotationsByType((Class<? extends Annotation>) args[0]);
            case "getDeclaredAnnotation" -> getDeclaredAnnotation((Class<? extends Annotation>) args[0]);
            case "getDeclaredAnnotationsByType" -> getDeclaredAnnotationsByType((Class<? extends Annotation>) args[0]);
            case "getDeclaredAnnotations" -> getDeclaredAnnotations();
            case "original" -> original();
            default -> super.invoke(proxy, method, args);
        };

    }
}
