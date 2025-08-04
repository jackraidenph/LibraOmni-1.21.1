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
        }

        Entry<Delegate, Object> delegateEntry = delegates.get(name);
        if (delegateEntry == null) {
            return super.invoke(proxy, method, args);
        }

        Delegate delegate = delegateEntry.getKey();

        Object val = delegateEntry.getValue();
        Object transformed = tryTransform(val, delegate);
        if (!val.equals(transformed)) {
            Class<?> oldReturnType = method.getReturnType();
            Class<?> newReturnType = (transformed instanceof Annotation annotation) ? annotation.annotationType() : transformed.getClass();
            if (!checkReturnType(oldReturnType, newReturnType)) {
                throw new IllegalStateException("Couldn't transform value [%s] of [%s], value of type [%s] is not applicable to [%s] "
                        .formatted(
                                delegate.attribute(),
                                original.annotationType(),
                                newReturnType.getName(),
                                oldReturnType.getName()
                        ));
            }
        }

        return tryBox(method.getReturnType(), transformed);
    }

    private boolean checkReturnType(Class<?> oldReturnType, Class<?> newReturnType) {
        return oldReturnType.isAssignableFrom(newReturnType)
                | (oldReturnType.isArray() && oldReturnType.componentType().isAssignableFrom(newReturnType));
    }

    private static Class<?> tryGetClass(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        Class<?> clazz = SafeReflectionUtil.forName(name);
        //Might be an inner class
        if (clazz == null) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot < 0) {
                return clazz;
            }
            name = name.substring(0, lastDot) + '$' + name.substring(lastDot + 1);
            clazz = SafeReflectionUtil.forName(name);
        }
        return clazz;
    }

    private Object tryTransform(Object original, Delegate delegate) throws IllegalStateException {
        Class<? extends Function<Object, Object>> transformerType;
        try {
            transformerType = delegate.transformer();
            if (transformerType.equals(NoOpTransformer.class)) {
                return original;
            }
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            if (typeMirror.toString().equals(NoOpTransformer.class.getCanonicalName())) {
                return original;
            }
            //noinspection unchecked
            transformerType = (Class<? extends Function<Object, Object>>) tryGetClass(typeMirror);
            if (transformerType == null) {
                throw new UnsupportedOperationException("""
                        Transformer class [%s] not found.
                        Most probably, you are trying to use custom transformer implementation
                        for a compile-time annotation.
                        This won't work, your transformer is not yet compiled.
                        """.formatted(typeMirror.toString()));
            }
        }

        try {
            Function<Object, Object> transformer = UnsafeReflectionUtil.tryConstruct(transformerType);
            return transformer.apply(original);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct transformer [%s]".formatted(transformerType.getName()), e);
        }
    }

    private String toStringProxy(Annotation proxy) {
        StringBuilder builder = new StringBuilder();
        builder.append('@').append(proxy.annotationType().getName());
        String methods = Arrays.stream(proxy.annotationType().getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(m -> methodToString(proxy, m))
                .collect(Collectors.joining(",", "(", ")"));
        return builder.append(methods).toString();
    }

    private String methodToString(Annotation proxy, Method annotationMethod) {
        Object val = invoke(proxy, annotationMethod, new Object[0]);
        String str = val.getClass().isArray() ? arrayToString((Object[]) val) : objToStr(val);
        return annotationMethod.getName() + "=" + str;
    }

    private String arrayToString(Object[] arr) {
        return arr.length == 0 ? "{}" : Arrays.stream(arr).map(this::objToStr).collect(Collectors.joining(",", "{", "}"));
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
