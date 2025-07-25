package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class AnnotatedElementInvocationHandler extends AnnotationCachingInvocationHandler<AnnotatedElement> {

    public AnnotatedElementInvocationHandler(AnnotatedElement original) {
        super(original, new ReflectionAnnotationAccessor(original));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if (name.equals("getAnnotations") || name.equals("getDeclaredAnnotations")) {
            return getProxiedRecursiveAnnotations();
        }

        return super.invoke(proxy, method, args);
    }
}
