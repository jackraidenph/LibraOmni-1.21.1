package dev.jackraidenph.libraomni.data.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

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

}
