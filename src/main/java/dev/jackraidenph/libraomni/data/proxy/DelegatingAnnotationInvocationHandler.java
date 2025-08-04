package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.annotation.Delegate.NoOpTransformer;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DelegatingAnnotationInvocationHandler extends ObjectPreservingInvocationHandler<Annotation> {

    private final Map<String, Entry<Delegate, Object>> delegates;

    public DelegatingAnnotationInvocationHandler(
            Annotation child,
            Map<String, Entry<Delegate, Object>> delegates
    ) {
        super(child);
        this.delegates = delegates;
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();

        if (name.equals("toString")) {
            return toStringProxy((Annotation) proxy);
        } else if (name.equals("annotationType")) {
            return original.annotationType();
        }

        Entry<Delegate, Object> delegate = delegates.get(name);
        if (delegate != null) {
            Object val = delegate.getValue();
            Object transformed = tryTransform(val, method, delegate.getKey());
            return tryBox(method.getReturnType(), transformed);
        }

        return super.invoke(proxy, method, args);
    }

    @SuppressWarnings("unchecked")
    private Object tryTransform(Object original, Method childMethod, Delegate delegate) throws IllegalStateException {
        Class<? extends Function<Object, Object>> transformerType;
        try {
            transformerType = delegate.transformer();
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            String name = typeMirror.toString();
            transformerType = (Class<? extends Function<Object, Object>>) SafeReflectionUtil.forName(name);
            if (transformerType == null) {
                int lastDot = name.lastIndexOf('.');
                name = name.substring(0, lastDot) + '$' + name.substring(lastDot + 1);
                transformerType = (Class<? extends Function<Object, Object>>) SafeReflectionUtil.forName(name);
            }
            if (transformerType == null) {
                throw new UnsupportedOperationException("""
                        Transformer class [%s] not found.
                        Most probably, you are trying to use custom transformer implementation
                        for a compile-time annotation.
                        This won't work, your transformer is not yet compiled.
                        """.formatted(typeMirror.toString()));
            }
        }

        Object val = original;
        if (!transformerType.equals(NoOpTransformer.class)) {
            try {
                Function<Object, Object> transformer = UnsafeReflectionUtil.tryConstruct(transformerType);
                val = transformer.apply(original);
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct transformer [%s]".formatted(transformerType.getName()));
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
