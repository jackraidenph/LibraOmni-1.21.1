package dev.jackraidenph.libraomni.annotation.runprocessing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceMapDeserializationHelper {

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

    private static final Map<String, Class<?>> CLASSES_CACHE = new HashMap<>();
    private static final Map<String, Record> RECORDS_CACHE = new HashMap<>();
    private static final Map<String, Annotation> ANNOTATIONS_CACHE = new HashMap<>();
    private static final Map<Class<?>, Map<String, Field>> FIELDS_CACHE = new HashMap<>();
    private static final Map<Class<?>, Map<String, Method>> METHODS_CACHE = new HashMap<>();
    private static final Map<Class<?>, Map<String, Constructor<?>>> CONSTRUCTORS_CACHE = new HashMap<>();

    public static Class<?> deserializeClass(String fullyQualified) {
        Class<?> primitive = PRIMITIVE_TYPES_MAP.get(fullyQualified);
        if (primitive != null) {
            return primitive;
        }

        Class<?> cached = CLASSES_CACHE.get(fullyQualified);
        if (cached != null) {
            return cached;
        }

        try {
            Class<?> clazz = Class.forName(fullyQualified);
            CLASSES_CACHE.put(fullyQualified, clazz);
            return clazz;
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(classNotFoundException);
        }
    }

    public static Method deserializeMethod(String methodName, Class<?> clazz, Class<?>[] parameterTypes) {
        String methodString = methodName + Arrays.toString(parameterTypes);

        Method cached = METHODS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).get(methodString);
        if (cached != null) {
            return cached;
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            METHODS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(methodString, method);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> Constructor<T> deserializeConstructor(Class<T> clazz, Class<?>[] parameterTypes) {
        String constructorString = Arrays.toString(parameterTypes);

        Constructor<?> cached = CONSTRUCTORS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).get(constructorString);
        if (cached != null) {
            return (Constructor<T>) cached;
        }

        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            CONSTRUCTORS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(constructorString, constructor);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Field deserializeField(String simpleName, Class<?> clazz) {
        Field cached = FIELDS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).get(simpleName);
        if (cached != null) {
            return cached;
        }

        try {
            Field constructor = clazz.getDeclaredField(simpleName);
            FIELDS_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(simpleName, constructor);
            return constructor;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Method deserializeMethod(String methodName, String clazz, List<String> parameters) {
        Class<?>[] parametersArray = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parametersArray[i] = deserializeClass(parameters.get(i));
        }

        return deserializeMethod(methodName, deserializeClass(clazz), parametersArray);
    }

    public static Field deserializeField(String simpleName, String clazz) {
        return deserializeField(simpleName, deserializeClass(clazz));
    }

    public static <T> Constructor<T> deserializeConstructor(String clazz, List<String> parameters) {
        Class<?>[] parametersArray = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parametersArray[i] = deserializeClass(parameters.get(i));
        }

        return deserializeConstructor((Class<T>) deserializeClass(clazz), parametersArray);
    }
}
