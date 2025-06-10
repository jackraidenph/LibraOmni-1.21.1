package dev.jackraidenph.libraomni.common.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.WeakHashMap;

public record ClassData(String name) implements AnnotatedReflectionData<Class<?>> {

    private static final Map<String, Class<?>> CACHE = new WeakHashMap<>();

    public ClassData(TypeElement element) {
        this(element.getQualifiedName().toString());
    }

    private static final Map<String, Class<?>> PRIMITIVE_TYPES_MAP = Map.of(
            "int", Integer.TYPE,
            "long", Long.TYPE,
            "short", Short.TYPE,
            "byte", Byte.TYPE,
            "boolean", Boolean.TYPE,
            "double", Double.TYPE,
            "float", Float.TYPE,
            "char", Character.TYPE
    );

    public static Class<?> classOrPrimitive(String name) {
        Class<?> primitive = PRIMITIVE_TYPES_MAP.get(name);
        if (primitive != null) {
            return primitive;
        }

        try {
            return Class.forName(name, false, ClassData.class.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(classNotFoundException);
        }
    }

    @Override
    public ElementKind kind() {
        return ElementKind.CLASS;
    }

    @Override
    public Class<?> construct() {
        return CACHE.computeIfAbsent(this.name(), ClassData::classOrPrimitive);
    }
}
