package dev.jackraidenph.libraomni.data.proxy;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.Map;

public abstract class ProxyFactory {

    public static <T extends Annotation, R extends Annotation> T proxifyAnnotation(T child, R parent) {
        if (parent == null) {
            return child;
        }
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                child.getClass().getClassLoader(),
                new Class[]{child.annotationType()},
                new DelegatingAnnotationInvocationHandler(child, parent)
        );
    }

    public static AnnotatedElement proxifyAnnotatedElement(AnnotatedElement element) {
        return (AnnotatedElement) Proxy.newProxyInstance(
                element.getClass().getClassLoader(),
                element.getClass().getInterfaces(),
                new AnnotatedElementInvocationHandler(element)
        );
    }

    public static AnnotatedConstruct proxifyAnnotatedConstruct(AnnotatedConstruct construct, Elements elementUtils) {
        return (AnnotatedConstruct) Proxy.newProxyInstance(
                construct.getClass().getClassLoader(),
                construct.getClass().getInterfaces(),
                new AnnotatedConstructInvocationHandler(construct, elementUtils)
        );
    }

    public static <T extends Annotation> T makeValueAnnotation(Class<T> type, Map<String, Object> attributes) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new AnnotationInvocationHandler(type, attributes)
        );
    }
}
