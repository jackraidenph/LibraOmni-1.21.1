package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.StringUtilities;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class DelegatingAnnotationInvocationHandler extends ObjectPreservingInvocationHandler<Annotation> {

    private final DelegateContainer delegateContainer;

    public DelegatingAnnotationInvocationHandler(Annotation original, DelegateContainer delegateContainer) {
        super(original);
        Set<String> nonExistent = delegateContainer.nonExistentMethods(original);
        if (!nonExistent.isEmpty()) {
            throw new IllegalStateException("Can't delegate methods %s from [%s] that don't exist in [%s]"
                    .formatted(nonExistent, delegateContainer.getDelegatorBinaryName(), original));
        }
        this.delegateContainer = delegateContainer;
    }

    private boolean checkReturnType(Class<?> oldReturnType, Class<?> newReturnType) {
        return oldReturnType.isAssignableFrom(newReturnType)
                | (oldReturnType.isArray() && oldReturnType.componentType().isAssignableFrom(newReturnType));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();

        if (name.equals("toString")) {
            return toStringProxy((Annotation) proxy);
        }

        if (!delegateContainer.hasDelegateFor(name)) {
            return super.invoke(proxy, method, args);
        }

        Object val = delegateContainer.getDelegatedValue(name);
        Object transformed = delegateContainer.getTransformedValue(name);
        if (!val.equals(transformed)) {
            Class<?> oldReturnType = method.getReturnType();
            Class<?> newReturnType = (transformed instanceof Annotation annotation) ? annotation.annotationType() : transformed.getClass();
            if (!checkReturnType(oldReturnType, newReturnType)) {
                throw new IllegalStateException("Couldn't transform value [%s] of [%s], value of type [%s] is not applicable to [%s] "
                        .formatted(
                                name,
                                original.annotationType(),
                                newReturnType.getName(),
                                oldReturnType.getName()
                        ));
            }
        }

        return SafeReflectionUtil.selfOrSingletonArray(method.getReturnType(), transformed);
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
