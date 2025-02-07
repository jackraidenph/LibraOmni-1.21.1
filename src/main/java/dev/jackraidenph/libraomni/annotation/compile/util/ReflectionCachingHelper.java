package dev.jackraidenph.libraomni.annotation.compile.util;

import dev.jackraidenph.libraomni.LibraOmni;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectionCachingHelper {

    public static final ReflectionCachingHelper INSTANCE = new ReflectionCachingHelper();

    private final Map<String, ClassData<?>> data = new HashMap<>();

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

    private ClassData<?> getClassDataByName(String name) {
        return this.getClassDataByClass(this.classOrPrimitive(name));
    }

    private <T> ClassData<T> getClassDataByClass(Class<T> clazz) {
        //Suppress, because the type is preserved
        //noinspection unchecked
        return (ClassData<T>) this.data.computeIfAbsent(clazz.getName(), n -> new ClassData<>(clazz));
    }

    public Class<?> getClassOrPrimitiveByName(String name) {
        return this.getClassDataByName(name).getClazz();
    }

    public Field getDeclaredField(Class<?> clazz, String name) {
        ClassData<?> classData = this.getClassDataByClass(clazz);
        if (classData.hasField(name)) {
            return classData.getField(name);
        } else {
            try {
                return classData.addField(name, clazz.getField(name));
            } catch (NoSuchFieldException noSuchFieldException) {
                throw new IllegalArgumentException(noSuchFieldException);
            }
        }
    }

    public Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        String qualifier = name + SerializationHelper.classesToString(parameterTypes);

        ClassData<?> classData = this.getClassDataByClass(clazz);
        if (classData.hasMethod(qualifier)) {
            return classData.getMethod(qualifier);
        } else {
            try {
                return classData.addMethod(qualifier, clazz.getMethod(name, parameterTypes));
            } catch (NoSuchMethodException noSuchMethodException) {
                throw new IllegalArgumentException(noSuchMethodException);
            }
        }
    }


    public <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        String qualifier = SerializationHelper.classesToString(parameterTypes);

        ClassData<T> classData = this.getClassDataByClass(clazz);
        if (classData.hasConstructor(qualifier)) {
            return classData.getConstructor(qualifier);
        } else {
            try {
                return classData.addConstructor(qualifier, clazz.getConstructor(parameterTypes));
            } catch (NoSuchMethodException noSuchMethodException) {
                throw new IllegalArgumentException(noSuchMethodException);
            }
        }
    }

    private static class ClassData<T> {

        private final Class<T> clazz;
        Map<String, Field> fields = new HashMap<>();
        Map<String, Method> methods = new HashMap<>();
        Map<String, Constructor<T>> constructors = new HashMap<>();

        public ClassData(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Class<T> getClazz() {
            return this.clazz;
        }

        public boolean hasField(String name) {
            return this.fields.containsKey(name);
        }

        public Field addField(String name, Field field) {
            this.fields.put(name, field);
            return field;
        }

        public Field getField(String name) {
            return this.fields.get(name);
        }

        public boolean hasMethod(String method) {
            return this.methods.containsKey(method);
        }

        public Method addMethod(String name, Method method) {
            this.methods.put(name, method);
            return method;
        }

        public Method getMethod(String name) {
            return this.methods.get(name);
        }

        public boolean hasConstructor(String name) {
            return this.fields.containsKey(name);
        }

        public Constructor<T> addConstructor(String name, Constructor<T> constructor) {
            this.constructors.put(name, constructor);
            return constructor;
        }

        public Constructor<T> getConstructor(String name) {
            return this.constructors.get(name);
        }
    }
}
