package dev.jackraidenph.libraomni.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

}
