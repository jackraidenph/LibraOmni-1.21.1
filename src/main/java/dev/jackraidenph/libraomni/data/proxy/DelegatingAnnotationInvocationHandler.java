package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DelegatingAnnotationInvocationHandler implements InvocationHandler {

    private final Annotation parent;
    private final Annotation child;
    private final Map<String, Method> delegateCache = new HashMap<>();

    public DelegatingAnnotationInvocationHandler(
            Annotation child,
            Annotation parent
    ) {
        this.child = child;
        this.parent = parent;
    }

    private Object safeBoxOrThrow(Class<?> expected, Object context, Method m) {
        Class<?> returnType = m.getReturnType();
        if (expected.isArray() && !returnType.isArray() && expected.componentType().isAssignableFrom(returnType)) {
            Object arr = Array.newInstance(returnType, 1);
            Object val = UnsafeReflectionUtil.getMethodValue(m, context);
            Array.set(arr, 0, val);
            return arr;
        } else if (expected.isAssignableFrom(returnType)) {
            return UnsafeReflectionUtil.getMethodValue(m, context);
        } else {
            throw new IllegalArgumentException("Can't delegate attribute of type [%s] to type [%s] (From [%s] to [%s])".formatted(
                    returnType, expected, m.getDeclaringClass(), context.getClass()
            ));
        }
    }

    private Method findDelegate(Annotation child, Annotation parent, String name) {
        if (delegateCache.containsKey(name)) {
            return delegateCache.get(name);
        }

        Class<? extends Annotation> type = parent.annotationType();
        for (Method m : type.getDeclaredMethods()) {
            Delegate delegate = m.getAnnotation(Delegate.class);
            if (delegate != null
                    && delegate.annotation().equals(child.annotationType())
                    && delegate.attribute().equals(name)
            ) {
                delegateCache.put(name, m);
                return m;
            }
        }
        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (!(proxy instanceof Annotation)) {
            throw new IllegalArgumentException("Can't invoke annotation-specific InvocationHandler for non-annotation");
        }
        String name = method.getName();
        Method delegate = findDelegate(child, parent, name);
        if (delegate != null) {
            return safeBoxOrThrow(method.getReturnType(), parent, delegate);
        }

        return UnsafeReflectionUtil.getMethodValue(method, child, args);
    }
}
