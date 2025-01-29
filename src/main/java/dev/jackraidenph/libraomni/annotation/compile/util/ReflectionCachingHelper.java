package dev.jackraidenph.libraomni.annotation.compile.util;

import dev.jackraidenph.libraomni.LibraOmni;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionCachingHelper {

    public static final ReflectionCachingHelper INSTANCE = new ReflectionCachingHelper();

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
            return Class.forName(name, false, LibraOmni.classLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(classNotFoundException);
        }
    }

    public Class<?> getClassOrPrimitiveByName(String string) {
        return classes.computeIfAbsent(string, this::classOrPrimitive);
    }

    public Field getDeclaredField(Class<?> clazz, String name) {
        try {
            if (!this.fields.computeIfAbsent(clazz, k -> new HashMap<>()).containsKey(name)) {
                Field field = clazz.getDeclaredField(name);
                this.fields.get(clazz).put(name, field);
                return field;
            } else {
                return this.fields.get(clazz).get(name);
            }
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new IllegalArgumentException(noSuchFieldException);
        }
    }

    public Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        String qualifier = name + SerializationHelper.classesToString(parameterTypes);
        try {
            if (!this.methods.computeIfAbsent(clazz, k -> new HashMap<>()).containsKey(qualifier)) {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                this.methods.get(clazz).put(qualifier, method);
                return method;
            } else {
                return this.methods.get(clazz).get(qualifier);
            }
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalArgumentException(noSuchMethodException);
        }
    }


    public <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        String qualifier = SerializationHelper.classesToString(parameterTypes);
        try {
            if (!this.constructors.computeIfAbsent(clazz, k -> new HashMap<>()).containsKey(qualifier)) {
                Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
                this.constructors.get(clazz).put(qualifier, constructor);
                return constructor;
            } else {
                //Suppress, because we safely recover from this
                //noinspection unchecked
                return (Constructor<T>) this.constructors.get(clazz).get(qualifier);
            }
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalArgumentException(noSuchMethodException);
        } catch (ClassCastException classCastException) {
            this.constructors.get(clazz).remove(qualifier);
            return this.getDeclaredConstructor(clazz, parameterTypes);
        }
    }
}
