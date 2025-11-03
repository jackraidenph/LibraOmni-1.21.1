package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;

public class ValueAnnotationInvocationHandler implements InvocationHandler, Annotation {

    private final Class<? extends Annotation> type;
    private final Map<String, Object> attributes;

    public ValueAnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> attributes) {
        for (Method m : type.getDeclaredMethods()) {
            int mods = m.getModifiers();
            if (!Modifier.isAbstract(mods) || m.isDefault()) {
                continue;
            }
            String mName = m.getName();
            if (!attributes.containsKey(mName)) {
                throw new IllegalArgumentException("Failed to create value annotation for type [%s], method [%s] is not filled".formatted(type, mName));
            }
        }
        this.type = type;
        this.attributes = attributes;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();

        switch (name) {
            case "toString" -> {
                return toString();
            }
            case "hashCode" -> {
                return hashCode();
            }
            case "equals" -> {
                return equals(args[0]);
            }
            case "annotationType" -> {
                return annotationType();
            }
        }

        Object val = attributes.get(name);
        if (val != null) {
            return val;
        }

        return method.getDefaultValue();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "ValueProxy@" + type.getName() + attributes.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Annotation annotation)) {
            return false;
        }

        if (!annotation.annotationType().equals(type)) {
            return false;
        }

        for (Method m : annotation.annotationType().getMethods()) {
            String name = m.getName();
            Object val = attributes.get(name);
            if (UnsafeReflectionUtil.getMethodValue(m, annotation).equals(val)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, attributes);
    }
}
