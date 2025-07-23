package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composite;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public abstract class AnnotationCachingInvocationHandler<T> extends ObjectPreservingInvocationHandler<T> {

    protected final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();

    public AnnotationCachingInvocationHandler(T original, AnnotationAccessor<T> accessor) {
        super(original);
        cacheRecursive(accessor);
    }

    protected Annotation[] getProxiedRecursiveAnnotations() {
        return annotationMap.values().toArray(Annotation[]::new);
    }

    private void cacheRecursive(AnnotationAccessor<T> accessor) {
        for (Annotation annotation : accessor.getAnnotations()) {
            step(annotation);
        }
    }

    private void step(Annotation parentAnnotation) {
        Class<? extends Annotation> type = parentAnnotation.annotationType();
        if (annotationMap.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate annotation type [%s] encountered during recursive lookup".formatted(type.getName()));
        }

        annotationMap.put(type, parentAnnotation);

        Composite composite = type.getAnnotation(Composite.class);

        if (composite == null) {
            return;
        }

        for (Annotation metaAnnotation : type.getAnnotations()) {
            if (!(metaAnnotation instanceof Composite)
                    && SafeReflectionUtil.sameRetentionAndTarget(parentAnnotation, metaAnnotation)
            ) {
                Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
                step(metaProxy);
            }
        }
    }
}
