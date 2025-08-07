package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedElementInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedElement> implements AnnotationAccessor<AnnotatedElement> {

    protected final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

    public AnnotatedElementInvocationHandler(AnnotatedElement original) {
        super(original);
        cacheRecursive(original.getAnnotations(), annotations);
        cacheRecursive(original.getDeclaredAnnotations(), declaredAnnotations);
    }

    @Override
    public AnnotatedElement annotatedObject() {
        return original;
    }

    public <T extends Annotation> T getAnnotationFrom(@NotNull Class<T> annotationClass, Map<Class<? extends Annotation>, List<Annotation>> map) {
        List<Annotation> byClass = map.get(annotationClass);
        if (byClass == null) {
            return null;
        }
        return annotationClass.cast(byClass.getFirst());
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        return getAnnotationFrom(annotationClass, annotations);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
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

    private void cacheRecursive(Annotation[] rootAnnotations, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
        for (Annotation annotation : rootAnnotations) {
            cacheStep(annotation, addTo);
        }
    }

    private void addAnnotation(Annotation annotation, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
        if (!(annotation instanceof Composed)) {
            Class<? extends Annotation> type = annotation.annotationType();
            addTo.computeIfAbsent(type, k -> new ArrayList<>()).add(annotation);
        }
    }

    private void cacheStep(Annotation parentAnnotation, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
        addAnnotation(parentAnnotation, addTo);

        Class<? extends Annotation> type = parentAnnotation.annotationType();
        Composed composed = type.getAnnotation(Composed.class);

        if (composed == null) {
            return;
        }

        for (Annotation metaAnnotation : type.getAnnotations()) {
            Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
            cacheStep(metaProxy, addTo);
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
            case "annotatedObject" -> annotatedObject();
            default -> super.invoke(proxy, method, args);
        };

    }
}
