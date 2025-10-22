package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedElementInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedElement> implements ProxyAnnotatedElement {

    protected final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

    public AnnotatedElementInvocationHandler(AnnotatedElement original) {
        super(original);
        //Cache directly accessible
        cacheRecursive(original.getAnnotations(), annotations, false);
        //Cache declared
        cacheRecursive(original.getDeclaredAnnotations(), declaredAnnotations, true);
    }

    @Override
    public AnnotatedElement original() {
        return original;
    }

    public <T extends Annotation> T getAnnotationFrom(@NotNull Class<T> annotationClass, Map<Class<? extends Annotation>, List<Annotation>> map) {
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

    private void failOnRepeatableContainer(Class<? extends Annotation> annotationType) {
        if (isRepeatableContainer(annotationType)) {
            throw new UnsupportedOperationException("Repeatable annotation containers are not supported for proxified annotations, please use #get(Declared)AnnotationsByType");
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        //Do not use computed cache for special-case service meta-annotations
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getAnnotation(annotationClass);
        }
        failOnRepeatableContainer(annotationClass);
        return getAnnotationFrom(annotationClass, annotations);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NotNull Class<T> annotationClass) {
        //Do not use computed cache for special-case service meta-annotations
        if (ProxyFactory.ONLY_DIRECT.contains(annotationClass)) {
            return original.getDeclaredAnnotation(annotationClass);
        }
        failOnRepeatableContainer(annotationClass);
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
            //No need to proxify first-level annotations, because they can't possibly have a parent annotation, unless the parent element itself is an annotation
            Annotation selfOrProxy = original instanceof Annotation parent
                    ? ProxyFactory.proxifyAnnotation(annotation, parent)
                    : annotation;
            cacheStep(selfOrProxy, addTo, declared);
        }
    }

    //Check if the annotation is a container for @Repeatable annotations specified as in https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    private boolean isRepeatableContainer(Class<? extends Annotation> annotationType) {
        Method[] attributes = annotationType.getMethods();
        //At least one actual attribute
        if (attributes.length < 1) {
            return false;
        }
        //Must contain "value" attribute
        Optional<Method> valueOptional = Arrays.stream(attributes).filter(m -> m.getName().equals("value")).findFirst();
        if (valueOptional.isEmpty()) {
            return false;
        }
        Method value = valueOptional.get();
        Class<?> returnType = value.getReturnType();
        //Must be an array of annotations
        if (!Annotation[].class.isAssignableFrom(returnType)) {
            return false;
        }
        //Array must contain annotations meta-annotated with @Repeatable
        Repeatable repeatable = (returnType.getComponentType()).getAnnotation(Repeatable.class);
        if (repeatable == null) {
            return false;
        }
        //Repeatable should specify the original annotation as a container
        return repeatable.value().equals(annotationType);
    }

    private List<Annotation> unwrapContainer(Annotation annotation) {
        if (!isRepeatableContainer(annotation.annotationType())) {
            throw new IllegalArgumentException("Not a container for @Repeatable");
        }
        try {
            Method value = annotation.annotationType().getMethod("value");
            return List.of(UnsafeReflectionUtil.getMethodValue(value, annotation));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addAnnotation(Annotation annotation, Map<Class<? extends Annotation>, List<Annotation>> addTo) {
        //Do not store special-case service meta-annotations in cache. They are grabbed as-is
        if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
            return;
        }

        List<Annotation> annotations = addTo.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>());
        if (!annotations.isEmpty()) {
            boolean repeatable = annotation.annotationType().getAnnotation(Repeatable.class) != null;
            //Prevent storing multiple non-repeatable annotations
            if (!repeatable) {
                throw new IllegalStateException("Annotation [%s] is not repeatable, previous encounter: [%s]".formatted(annotation, annotations.getFirst()));
            }
        }
        annotations.add(annotation);
    }

    private void cacheStep(Annotation currentAnnotation, Map<Class<? extends Annotation>, List<Annotation>> addTo, boolean declared) {
        if (isRepeatableContainer(currentAnnotation.annotationType())) {
            //Unwrap container's contents as a separate set of annotations, ignoring the container annotation itself
            for (Annotation inContainer : unwrapContainer(currentAnnotation)) {
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
            if (!ProxyFactory.ONLY_DIRECT.contains(metaAnnotation.annotationType())
                    && type.getAnnotation(Repeatable.class) != null
                    && metaAnnotation.annotationType().getAnnotation(Repeatable.class) == null) {
                throw new IllegalStateException("Type [%s] is marked as @Repeatable, but its meta-annotation [%s] is not".formatted(type, metaAnnotation.annotationType()));
            }
            Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, currentAnnotation);
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
