package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AnnotatedConstructInvocationHandler extends AnnotationCachingInvocationHandler<AnnotatedConstruct> {

    private final Elements elementUtils;

    public AnnotatedConstructInvocationHandler(AnnotatedConstruct original, Elements elementUtils) {
        super(original, new JavaLangAnnotationAccessor(original));
        this.elementUtils = elementUtils;
    }

    private <A extends Annotation> A[] byType(Class<A> clazz) {
        Object arr;
        Object annotation = annotationMap.get(clazz);
        if (annotation != null) {
            arr = Array.newInstance(clazz, 1);
            Array.set(arr, 0, annotation);
            //noinspection unchecked
            return (A[]) arr;
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

    private List<? extends AnnotationMirror> annotationMirrors() {
        List<AnnotationMirror> mirrors = new ArrayList<>();
        for (Class<? extends Annotation> annotationClazz : annotationMap.keySet()) {
            String name = annotationClazz.getName();
            TypeElement typeElement = elementUtils.getTypeElement(name);
            if (typeElement == null) {
                continue;
            }
            TypeMirror typeMirror = typeElement.asType();
            if (typeMirror instanceof AnnotationMirror annotationMirror) {
                mirrors.add(annotationMirror);
            }
        }

        return mirrors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAnnotationMirrors" -> annotationMirrors();
            case "getAnnotation" -> byTypeSingular((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByType" -> byType((Class<? extends Annotation>) args[0]);
            case null -> throw new IllegalStateException();
            default -> UnsafeReflectionUtil.getMethodValue(method, original, args);
        };
    }
}
