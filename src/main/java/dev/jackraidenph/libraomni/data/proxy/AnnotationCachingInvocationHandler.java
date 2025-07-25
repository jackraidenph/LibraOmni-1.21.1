package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AnnotationCachingInvocationHandler<T> extends ObjectPreservingInvocationHandler<T> implements AnnotationAccessor<T> {

    protected final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private final AnnotationAccessor<T> accessor;

    public AnnotationCachingInvocationHandler(T original, AnnotationAccessor<T> accessor) {
        super(original);
        this.accessor = accessor;
        cacheRecursive(accessor);
    }

    @Override
    public T annotatedObject() {
        return original;
    }

    @Override
    public Collection<Annotation> getAllAnnotations() {
        return accessor.getAllAnnotations();
    }

    protected Annotation[] getProxiedRecursiveAnnotations() {
        return annotationMap.values().toArray(Annotation[]::new);
    }

    private void cacheRecursive(AnnotationAccessor<T> accessor) {
        for (Annotation annotation : accessor.getAllAnnotations()) {
            step(annotation);
        }
    }

    private void step(Annotation parentAnnotation) {
        Class<? extends Annotation> type = parentAnnotation.annotationType();
        if (annotationMap.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate annotation type [%s] encountered during recursive lookup".formatted(type.getName()));
        }

        annotationMap.put(type, parentAnnotation);

        Composed composed = type.getAnnotation(Composed.class);

        if (composed == null) {
            return;
        }

        for (Annotation metaAnnotation : type.getAnnotations()) {
            if (!(metaAnnotation instanceof Composed)
                    && SafeReflectionUtil.sameRetentionAndTarget(parentAnnotation, metaAnnotation)
            ) {
                Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
                step(metaProxy);
            }
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
