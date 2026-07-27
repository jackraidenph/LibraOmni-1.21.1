package dev.jackraidenph.libraomni.data.proxy.runtime;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.AbstractInterceptorProxy;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

public class SyntheticAnnotation<T extends Annotation> extends AbstractInterceptorProxy {

    private final Class<T> type;
    private final Map<String, Object> attributes;

    public static <T extends Annotation> T create(Class<? extends T> type, Map<String, Object> attributeValues) {
        return ProxyFactory.sythesizeAnnotation(type, attributeValues);
    }

    public SyntheticAnnotation(Class<T> type, Map<String, Object> attributes) {
        super();

        this.type = type;
        this.attributes = attributes == null ? Map.of() : attributes;

        for (Method m : type.getDeclaredMethods()) {
            int mods = m.getModifiers();
            if (!Modifier.isAbstract(mods) || m.getDefaultValue() != null) {
                continue;
            }
            String mName = m.getName();
            if (!this.attributes.containsKey(mName)) {
                throw new IllegalArgumentException("Failed to create value annotation for type [%s], attribute [%s] is not filled".formatted(type, mName));
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (hasInterceptorsFor(method)) {
            return super.invoke(proxy, method, args);
        }

        String name = method.getName();
        Object val = attributes.get(name);
        if (val == null) {
            return method.getDefaultValue();
        }

        if (val instanceof RuntimeException e) {
            throw e;
        }

        return val;
    }

    @InterceptorFor("annotationType")
    public Class<? extends Annotation> annotationType() {
        return this.type;
    }

    @Override
    @InterceptorFor("toString")
    public String toString() {
        return "Synthetic@" + stringIdentity(annotationType().getName(), attributes, false);
    }

    public static String stringIdentity(String typeName, Map<String, Object> attributes, boolean sort) {
        StringJoiner argsJoiner = new StringJoiner(",", "(", ")");

        List<Entry<String, Object>> l = new ArrayList<>(attributes.entrySet());

        if (sort) {
            l.sort(Comparator.comparingInt(e -> e.getKey().charAt(0)));
        }

        for (Entry<String, Object> e : l) {
            String name = e.getKey();
            Object value = e.getValue();

            if (value == null) {
                continue;
            }

            String valueStr = value.getClass().isArray() ? SafeReflectionUtil.arrayToString(value) : value.toString();

            argsJoiner.add(name + "=" + valueStr);
        }

        return typeName + argsJoiner;
    }

    @Override
    @InterceptorFor("equals")
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
    @InterceptorFor("hashCode")
    public int hashCode() {
        return Objects.hash(type, attributes);
    }
}
