package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractInterceptorProxy implements InvocationHandler {
    private final Map<String, List<Method>> interceptors;

    protected AbstractInterceptorProxy() {
        Map<String, List<Method>> map = new HashMap<>();
        for (Method m : getClass().getDeclaredMethods()) {
            for (InterceptorFor target : m.getAnnotationsByType(InterceptorFor.class)) {
                map.computeIfAbsent(target.value(), k -> new ArrayList<>()).add(m);
            }
        }

        interceptors = map;
    }

    protected boolean hasInterceptorsFor(Method method) {
        return interceptors.containsKey(method.getName());
    }

    private Method findInterceptorForArgs(List<Method> methods, Object[] args) {
        Class<?>[] types = SafeReflectionUtil.inferTypes(args);

        for (Method declared : methods) {
            Class<?>[] parameterTypes = declared.getParameterTypes();

            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(types[i])) {
                    matches = false;
                    break;
                }
            }

            if (!matches) {
                continue;
            }

            return declared;
        }

        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();

        Method ownMethod = findInterceptorForArgs(interceptors.get(methodName), args);

        if (ownMethod == null) {
            throw new IllegalArgumentException("No method interceptor found for method [%s] with parameter types [%s]"
                    .formatted(methodName, SafeReflectionUtil.inferTypes(args))
            );
        }

        ownMethod.setAccessible(true);
        try {
            return ownMethod.invoke(this, args);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "InvocationHandler [%s] failed to invoke own method interceptor for method [%s]"
                            .formatted(this.getClass().getName(), method.getName()), e
            );
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
