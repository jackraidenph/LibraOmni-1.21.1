package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.annotation.Delegate.NoOpTransformer;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DelegatingAnnotationInvocationHandler extends ObjectPreservingInvocationHandler<Annotation> {

    private final Annotation parent;
    private final Map<String, Entry<Delegate, Method>> delegateCache = new HashMap<>();

    public DelegatingAnnotationInvocationHandler(
            Annotation child,
            Annotation parent
    ) {
        super(child);
        this.parent = parent;
    }

    private Object tryBox(Class<?> expected, Object val) {
        Class<?> type = SafeReflectionUtil.selfOrAnnotationType(val);
        if (expected.isArray() && !type.isArray() && expected.componentType().isAssignableFrom(type)) {
            Object arr = Array.newInstance(type, 1);
            Array.set(arr, 0, val);
            return arr;
        } else {
            return val;
        }
    }

    private Entry<Delegate, Method> findDelegate(Annotation child, Annotation parent, String name) {
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
                Entry<Delegate, Method> pair = Map.entry(delegate, m);
                delegateCache.put(name, pair);
                return pair;
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

        Entry<Delegate, Method> delegatePair = findDelegate(original, parent, name);
        if (delegatePair != null) {
            Object val = UnsafeReflectionUtil.getMethodValue(delegatePair.getValue(), parent);
            return tryBox(method.getReturnType(), tryTransform(val, method, delegatePair.getKey()));
        }

        return UnsafeReflectionUtil.getMethodValue(method, original, args);
    }

    private Object tryTransform(Object original, Method childMethod, Delegate delegate) throws IllegalStateException {
        Class<? extends Function<Object, Object>> transformerType = delegate.transformer();

        Object val = original;
        if (!transformerType.equals(NoOpTransformer.class)) {
            try {
                Function<Object, Object> transformer = UnsafeReflectionUtil.tryConstruct(transformerType);
                val = transformer.apply(original);
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct transformer [%s] for [%s]"
                        .formatted(transformerType.getName(), parent.annotationType().getName())
                );
            }
        }

        Class<?> originalReturnType = childMethod.getReturnType();

        Class<?> newReturnType = (val instanceof Annotation annotation)
                ? annotation.annotationType()
                : val.getClass();

        boolean isApplicable = originalReturnType.isAssignableFrom(newReturnType)
                | (originalReturnType.isArray() && originalReturnType.componentType().isAssignableFrom(newReturnType));

        if (!isApplicable) {
            throw new IllegalStateException("[Transformer %s] Value of type [%s] is not applicable to [%s] of [%s]"
                    .formatted(
                            transformerType.getDeclaringClass().getSimpleName() + "$" + transformerType.getSimpleName(),
                            newReturnType.getName(),
                            originalReturnType.getName(),
                            childMethod.getDeclaringClass()
                    )
            );
        }

        return val;
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
