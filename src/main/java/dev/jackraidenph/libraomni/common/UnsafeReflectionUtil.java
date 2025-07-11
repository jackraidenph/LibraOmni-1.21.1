package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;

import java.lang.reflect.*;

public class UnsafeReflectionUtil {

    public static <T> T getValue(AnnotatedElement annotatedElement, Object context, Object... args) {
        return switch (annotatedElement) {
            case TransitiveAnnotatedElement transitiveAnnotatedElement ->
                    getValue(transitiveAnnotatedElement.getAnnotatedElement(), context, args);
            case AccessibleObject accessibleObject -> getValue(accessibleObject, context, args);
            case Class<?> clazz -> {
                try {
                    //noinspection unchecked
                    yield (T) tryConstruct(clazz, args);
                } catch (ClassCastException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    public static <T> T getValue(AccessibleObject accessibleObject, Object context, Object... args) {
        accessibleObject.setAccessible(true);
        return switch (accessibleObject) {
            case Field field -> getFieldValue(field, context);
            case Method method -> getMethodValue(method, context, args);
            case Constructor<?> constructor -> {
                try {
                    //noinspection unchecked
                    yield (T) getConstructorValue(constructor, args);
                } catch (ClassCastException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    public static <T> T getMethodValue(Method method, Object context, Object... args) {
        try {
            //Return null if the return type is not appropriate
            //noinspection unchecked
            return (T) method.invoke(context, args);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getFieldValue(Field field, Object context) {
        try {
            //Return null if the return type is not appropriate
            //noinspection unchecked
            return (T) field.get(context);
        } catch (IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFieldValueStatic(Field field) {
        return getFieldValue(field, null);
    }

    public static <T> T getConstructorValue(Constructor<T> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("There was an exception inside the empty constructor", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to instantiate the class, InstantiationException was thrown. Check that your class is not abstract or interface");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get access to the empty constructor");
        }
    }

    public static <T> T tryConstruct(Class<T> clazz, Object... args) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(inferTypes(args));
            return getConstructorValue(constructor, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?>[] inferTypes(Object... objects) {
        Class<?>[] typesArray = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            typesArray[i] = objects[i].getClass();
        }

        return typesArray;
    }
}
