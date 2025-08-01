package dev.jackraidenph.libraomni.data.proxy;

import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

public class AnnotatedConstructInvocationHandler extends AnnotationMirrorCachingInvocationHandler {


    public AnnotatedConstructInvocationHandler(AnnotatedConstruct original) {
        super(original);
    }

    private <A extends Annotation> A[] byType(Class<A> clazz) {
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
