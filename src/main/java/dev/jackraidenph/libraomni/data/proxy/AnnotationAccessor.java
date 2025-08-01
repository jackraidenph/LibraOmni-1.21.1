package dev.jackraidenph.libraomni.data.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public interface AnnotationAccessor<T> {
    Collection<Annotation> getAllAnnotations();

    default <A extends Annotation> A getAnnotationByClass(Class<A> annotationType) {
        List<A> annotations = getAnnotationsByClass(annotationType);
        return annotations.isEmpty() ? null : annotations.getFirst();
    }

    @SuppressWarnings("unchecked")
    default <A extends Annotation> List<A> getAnnotationsByClass(Class<A> annotationType) {
        return getAllAnnotations().stream().filter(a -> a.annotationType().equals(annotationType)).map(a -> (A) a).toList();
    }

    default boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotationByClass(annotationType) != null;
    }

    AnnotatedElement annotatedObject();
}
