package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedElementInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedElement> implements ProxyAnnotatedElement {

    private final AnnotatedElementCache cache;
    private final ModMetadataReader modMetadataReader;

    public AnnotatedElementInvocationHandler(AnnotatedElement original, ModMetadataReader modMetadataReader) {
        super(original);
        this.cache = new AnnotatedElementCache();
        this.modMetadataReader = modMetadataReader;
    }

    @Override
    public AnnotatedElement original() {
        return original;
    }

    public <T extends Annotation> T getAnnotationFrom(@NotNull Class<T> annotationClass, Map<Class<? extends Annotation>, List<Annotation>> map) {
        if (SafeReflectionUtil.isRepeatableContainer(annotationClass)) {
            throw new UnsupportedOperationException("Repeatable annotation containers are not supported for proxified annotations, please use #get(Declared)AnnotationsByType");
        }

        //All the cached instances of a class
        List<Annotation> byClass = map.get(annotationClass);
        if (byClass == null || byClass.isEmpty()) {
            return null;
        }
        if (annotationClass.getAnnotation(Repeatable.class) != null && byClass.size() > 1) {
            throw new UnsupportedOperationException("Please use #get(Declared)AnnotationsByType to get multiple instances of a @Repeatable annotation");
        }
        //At this point we are sure that the there's one and only one instance in the map
        return annotationClass.cast(byClass.getFirst());
    }

    /// OVERRIDES

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        //Do not use computed cache for special-case service meta-annotations
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getAnnotation(annotationClass);
        }
        return getAnnotationFrom(annotationClass, cache.annotations);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        //Do not use computed cache for special-case service meta-annotations
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getDeclaredAnnotation(annotationClass);
        }
        return getAnnotationFrom(annotationClass, cache.declaredAnnotations);
    }

    @Override
    public @NotNull Annotation[] getAnnotations() {
        return cache.annotations.values().stream().flatMap(List::stream).toArray(Annotation[]::new);
    }

    @Override
    public @NotNull Annotation[] getDeclaredAnnotations() {
        return cache.declaredAnnotations.values().stream().flatMap(List::stream).toArray(Annotation[]::new);
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

    /// CACHE IMPL

    private class AnnotatedElementCache {

        private final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
        private final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

        private AnnotatedElementCache() {
            AnnotatedElement element = original;
            //Cache directly accessible
            cacheRecursive(element.getAnnotations(), annotations, false);
            //Cache declared
            cacheRecursive(element.getDeclaredAnnotations(), declaredAnnotations, true);
        }

        private void cacheRecursive(Annotation[] rootAnnotations, Map<Class<? extends Annotation>, List<Annotation>> addTo, boolean declared) {
            for (Annotation annotation : rootAnnotations) {
                //No need to proxify first-level annotations, because they can't possibly have a parent annotation, unless the parent element itself is an annotation
                Annotation selfOrProxy = original instanceof Annotation parent
                        ? ProxyFactory.proxifyAnnotation(annotation, parent, original, modMetadataReader)
                        : annotation;
                cacheStep(selfOrProxy, addTo, declared);
            }
        }

        private void cacheStep(Annotation currentAnnotation, Map<Class<? extends Annotation>, List<Annotation>> addTo, boolean declared) {
            if (SafeReflectionUtil.isRepeatableContainer(currentAnnotation.annotationType())) {
                //Unwrap container's contents as a separate set of annotations, ignoring the container annotation itself
                for (Annotation inContainer : SafeReflectionUtil.unwrapRepeatableContainer(currentAnnotation)) {
                    //Should move container's meta-annotations to its contents? I don't think so
                    cacheStep(inContainer, addTo, declared);
                }
                return;
            } else {
                addAnnotation(currentAnnotation, addTo);
            }

            Class<? extends Annotation> type = currentAnnotation.annotationType();

            if (type.getAnnotation(Composed.class) == null) {
                return;
            }

            Annotation[] metaAnnotations = declared ? type.getDeclaredAnnotations() : type.getAnnotations();
            for (Annotation metaAnnotation : metaAnnotations) {
                if (repeatableViolation(currentAnnotation, metaAnnotation)) {
                    throw new IllegalStateException("Type [%s] is marked as @Repeatable, but its meta-annotation [%s] is not".formatted(type, metaAnnotation.annotationType()));
                }

                cacheStep(
                        ProxyFactory.proxifyAnnotation(metaAnnotation, currentAnnotation, original, modMetadataReader),
                        addTo,
                        declared
                );
            }
        }

        private boolean repeatableViolation(Annotation current, Annotation meta) {
            return !ProxyFactory.ONLY_DIRECT.contains(meta.annotationType())
                    && current.annotationType().getAnnotation(Repeatable.class) != null
                    && meta.annotationType().getAnnotation(Repeatable.class) == null;
        }

        private void addAnnotation(Annotation annotation, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
            //Do not store special-case service meta-annotations in cache. They are grabbed as-is
            if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
                return;
            }

            List<Annotation> annotations = addTo.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>());
            annotations.add(annotation);
        }
    }
}
