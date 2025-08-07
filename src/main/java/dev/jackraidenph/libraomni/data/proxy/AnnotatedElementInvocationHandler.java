package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class AnnotatedElementInvocationHandler extends AnnotationCachingInvocationHandler {

    public AnnotatedElementInvocationHandler(AnnotatedElement original) {
        super(original);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
//        String name = method.getName();
//        if (name.equals("getAnnotations")) {
//            return getAnnotations();
//        } else if (name.equals("getDeclaredAnnotations")) {
//            return getDeclaredAnnotations();
//        }

        return super.invoke(proxy, method, args);
    }
}
