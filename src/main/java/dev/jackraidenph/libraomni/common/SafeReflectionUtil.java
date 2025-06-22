package dev.jackraidenph.libraomni.common;

import java.lang.annotation.ElementType;
import java.lang.reflect.*;

/**
 * Instead of throwing, methods return null.
 * If throwing an exception is actually needed - null check should be used on the calling side
 */
public class SafeReflectionUtil {

    public static Class<?> forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static <T> Class<? extends T> forNameSubclass(String name, Class<T> clazz) {
        Class<?> forNameClass = forName(name);
        if (forNameClass == null) {
            return null;
        }
        return forNameClass.asSubclass(clazz);
    }

    public static <T> T tryConstruct(Class<? extends T> clazz, Object... args) {
        try {
            Constructor<? extends T> constructor = clazz.getDeclaredConstructor(inferTypes(args));
            return constructor.newInstance(args);
        } catch (NoSuchMethodException
                 | InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException e
        ) {
            return null;
        }
    }

    private static Class<?>[] inferTypes(Object... objects) {
        Class<?>[] typesArray = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            typesArray[i] = objects[i].getClass();
        }

        return typesArray;
    }

    public static ElementType getElementType(AnnotatedElement element) {
        if (element == null) {
            return null;
        }

        return switch (element) {
            case Class<?> clazz -> clazz.isAnnotation() ? ElementType.ANNOTATION_TYPE : ElementType.TYPE;
            case Method ignored -> ElementType.METHOD;
            case Field ignored -> ElementType.FIELD;
            case Constructor<?> ignored -> ElementType.CONSTRUCTOR;
            case Parameter ignored -> ElementType.PARAMETER;
            case Package ignored -> ElementType.PACKAGE;
            case Module ignored -> ElementType.MODULE;
            case RecordComponent ignored -> ElementType.RECORD_COMPONENT;
            default -> null;
        };
    }
}
