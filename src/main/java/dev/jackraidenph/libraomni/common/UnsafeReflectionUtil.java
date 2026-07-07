package dev.jackraidenph.libraomni.common;

import javax.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.Arrays;

public class UnsafeReflectionUtil {

    public static boolean isIntefaceMethodOverriden(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("Failed to find the desired method, perhaps, the signature is wrong?");
        }

        try {
            Method method = clazz.getMethod(methodName, paramTypes);
            if (!method.isDefault()) {
                return true;
            }

            return !method.getDeclaringClass().isInterface();
        } catch (NoSuchMethodException e) {
            return isIntefaceMethodOverriden(clazz.getSuperclass(), methodName, paramTypes);
        }
    }

    @Nonnull
    public static <T> T instantiateStatic(AnnotatedElement object, Object... args) {
        try {
            T created = UnsafeReflectionUtil.getValue(object, null, true, args);

            if (created == null) {
                throw new IllegalStateException("Failed to instantiate object from element [%s]".formatted(object.toString()));
            }
            return created;
        } catch (IllegalArgumentException illegalArgumentException) {
            if (SafeReflectionUtil.isExecutable(object)) {
                String actual = Arrays.toString(SafeReflectionUtil.getMethodParameters(object));
                String expected = Arrays.toString(SafeReflectionUtil.inferTypes(args));
                throw new IllegalStateException("Expected executable with parameters %s, got %s".formatted(expected, actual));
            }
            throw new IllegalStateException(illegalArgumentException);
        }
    }

    public static void tryInject(Field filed, Object value) {
        tryInject(filed, null, value);
    }

    public static void tryInject(Field field, Object context, Object value) {
        int mods = field.getModifiers();
        if (context == null && !Modifier.isStatic(mods)) {
            throw new IllegalArgumentException("""
                    Trying to inject [%s] into a non-static field [%s],
                    make it static
                    """.formatted(value, field.getName()));
        }

        if (Modifier.isFinal(mods)) {
            throw new IllegalArgumentException("""
                    Trying to inject [%s] into a final field [%s],
                    make it not final
                    """.formatted(value, field.getName()));
        }

        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException illegalAccessException) {
            throw new RuntimeException();
        }
    }

    public static <T> T getValue(AnnotatedElement annotatedElement, Object context, boolean resolveFunctionalFields, Object... args) {
        return switch (annotatedElement) {
            case AccessibleObject accessibleObject ->
                    getValueFromAccessible(accessibleObject, context, resolveFunctionalFields, args);
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

    public static <T> T getValueFromAccessible(AccessibleObject accessibleObject, Object context, boolean resolveFunctionalFields, Object... args) {
        accessibleObject.setAccessible(true);
        return switch (accessibleObject) {
            case Field field -> getFieldValue(field, context, resolveFunctionalFields, args);
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
            if (context == null && !Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Trying to get value statically from a non-static method [%s]".formatted(method.getName()));
            }

            //noinspection unchecked
            return (T) method.invoke(context, args);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getFieldValue(Field field, Object context, boolean resolveFunctionalInterfaces, Object... args) {
        try {
            if (context == null && !Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Trying to get value statically from a non-static field [%s]".formatted(field.getName()));
            }

            Object val = field.get(context);

            if (resolveFunctionalInterfaces) {
                Class<?> clazz = SafeReflectionUtil.selfOrReturnType(field);
                if (SafeReflectionUtil.isFunctionalInterface(clazz)) {
                    return getMethodValue(clazz.getMethods()[0], val, args);
                }
            }

            //noinspection unchecked
            return (T) val;
        } catch (IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
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
            Constructor<T> constructor = clazz.getDeclaredConstructor(SafeReflectionUtil.inferTypes(args));
            return getConstructorValue(constructor, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
