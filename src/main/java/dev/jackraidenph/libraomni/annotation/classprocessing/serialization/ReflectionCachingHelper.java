package dev.jackraidenph.libraomni.annotation.classprocessing.serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionCachingHelper {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Map<Class<?>, Map<String, Field>> fields = new HashMap<>();
    private final Map<Class<?>, Map<String, Method>> methods = new HashMap<>();
    private final Map<Class<?>, Map<String, Constructor<?>>> constructors = new HashMap<>();

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

    private Class<?> classOrPrimitive(String name) {
        Class<?> primitive = PRIMITIVE_TYPES_MAP.get(name);
        if (primitive != null) {
            return primitive;
        }

        try {
            return Class.forName(name, false, null);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(classNotFoundException);
        }
    }

    public Class<?> getClassOrPrimitiveByName(String string) {
        return classes.computeIfAbsent(string, this::classOrPrimitive);
    }

    public Field getDeclaredField(Class<?> clazz, String name) {
        try {
            return this.fields
                    .computeIfAbsent(clazz, k -> new HashMap<>())
                    .put(name, clazz.getDeclaredField(name));
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new IllegalArgumentException(noSuchFieldException);
        }
    }

    public Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return this.methods
                    .computeIfAbsent(clazz, k -> new HashMap<>())
                    .put(name, clazz.getDeclaredMethod(name, parameterTypes));
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalArgumentException(noSuchMethodException);
        }
    }

    public Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return this.constructors
                    .computeIfAbsent(clazz, k -> new HashMap<>())
                    .put(SerializationHelper.classesToString(parameterTypes), clazz.getDeclaredConstructor(parameterTypes));
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalArgumentException(noSuchMethodException);
        }
    }
}
