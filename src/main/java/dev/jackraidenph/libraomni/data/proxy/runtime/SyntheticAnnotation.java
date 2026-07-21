package dev.jackraidenph.libraomni.data.proxy.runtime;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.AbstractInterceptorProxy;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

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

    private boolean containsNonDefaultMethods() {
        return Arrays.stream(annotationType().getDeclaredMethods()).map(Method::getDefaultValue).anyMatch(Objects::isNull);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (hasInterceptorsFor(method)) {
            return super.invoke(proxy, method, args);
        }

        String name = method.getName();
        Object val = attributes.get(name);
        if (val != null) {
            return val;
        }

        return method.getDefaultValue();
    }

    @InterceptorFor("annotationType")
    public Class<? extends Annotation> annotationType() {
        return this.type;
    }

    @Override
    @InterceptorFor("toString")
    public String toString() {
        return "Synthetic@" + type.getName() + attributesToString(attributes);
    }

    private static String attributesToString(Map<String, Object> attributes) {
        StringJoiner builder = new StringJoiner(",", "(", ")");
        for (Entry<String, Object> e : attributes.entrySet()) {
            Object v = e.getValue();
            String vStr = v.getClass().isArray() ? Arrays.toString((Object[]) e.getValue()) : String.valueOf(v);
            builder.add(e.getKey() + "(" + vStr + ")");
        }
        return builder.toString();
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
