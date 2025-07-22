package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class DelegatingAnnotationInvocationHandler extends ObjectPreservingInvocationHandler<Annotation> {

    private final Annotation parent;
    private final Map<String, Method> delegateCache = new HashMap<>();

    public DelegatingAnnotationInvocationHandler(
            Annotation child,
            Annotation parent
    ) {
        super(child);
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
        String name = method.getName();

        if (name.equals("toString")) {
            return toStringProxy((Annotation) proxy);
        }

        Method delegate = findDelegate(original, parent, name);
        if (delegate != null) {
            return safeBoxOrThrow(method.getReturnType(), parent, delegate);
        }

        return UnsafeReflectionUtil.getMethodValue(method, original, args);
    }

    private String toStringProxy(Annotation proxy) {
        StringBuilder builder = new StringBuilder();

        builder.append('@').append(proxy.annotationType().getName());

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Method method : proxy.annotationType().getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                joiner.add(methodToString(proxy, method));
            }
        }

        builder.append(joiner);
        return builder.toString();
    }

    private String methodToString(Annotation proxy, Method annotationMethod) {
        String name = annotationMethod.getName();
        Object val = invoke(proxy, annotationMethod, new Object[0]);

        String str;
        if (val.getClass().isArray()) {
            str = arrayToString((Object[]) val);
        } else {
            str = objToStr(val);
        }

        return name + "=" + str;
    }


    private String arrayToString(Object[] arr) {
        if (arr.length == 0) {
            return "{}";
        }

        return Arrays.stream(arr).map(this::objToStr).collect(Collectors.joining(",", "{", "}"));
    }

    private String objToStr(Object o) {
        if (o instanceof String str) {
            return StringUtilities.quote(str);
        } else if (o instanceof Class<?> clazz) {
            return clazz.getName();
        }

        return String.valueOf(o);
    }
}
