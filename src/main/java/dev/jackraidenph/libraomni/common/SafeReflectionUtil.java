package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.annotation.Registered;

import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.util.Set;

/**
 * Instead of throwing, methods return null.
 * If throwing an exception is actually needed - null check should be used on the calling side
 */
public class SafeReflectionUtil {

    public static Type[] extractTypeArgumentsFromFunctionalField(Field field) {
        Class<?> type = field.getType();
        Method function = type.getMethods()[0];
        return function.getParameterTypes();
    }

    public static Class<?> tryResolveFunctionalReturnType(Field functionalTypeField) {
        Type genericType = functionalTypeField.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }

        Type[] types = parameterizedType.getActualTypeArguments();
        if (types.length < 1) {
            return null;
        }

        return (Class<?>) types[types.length - 1];
    }

    public static boolean isFunctionalInterface(AnnotatedElement element) {
        if (!(element instanceof Class<?> clazz)) {
            return false;
        }

        if (!clazz.isInterface()) {
            return false;
        }

        Method[] methods = clazz.getMethods();
        boolean encounteredAbstract = false;
        for (Method m : methods) {
            if (!Modifier.isAbstract(m.getModifiers())) {
                continue;
            }
            if (encounteredAbstract) {
                return false;
            }
            encounteredAbstract = true;
        }

        return encounteredAbstract;
    }

    public static Class<?> selfOrReturnType(AnnotatedElement element) {
        return selfOrReturnType(element, false);
    }

    public static Class<?> selfOrReturnType(AnnotatedElement element, boolean resolveFunctionalInterfaces) {
        return switch (element) {
            case Class<?> clazz -> clazz;
            case Field field -> {
                Class<?> clazz = field.getType();
                if (!resolveFunctionalInterfaces || !isFunctionalInterface(clazz)) {
                    yield clazz;
                }
                yield tryResolveFunctionalReturnType(field);
            }
            case Method method -> method.getReturnType();
            case null, default -> throw new UnsupportedOperationException();
        };
    }

    public static Class<?> declaringOrSelf(AnnotatedElement e) {
        return switch (e) {
            case Class<?> clazz -> clazz;
            case Member member -> member.getDeclaringClass();
            case null, default -> throw new UnsupportedOperationException();
        };
    }

    public static String objectName(AnnotatedElement element) {
        return StringUtilities.snakeCase(
                switch (element) {
                    case Class<?> clazz -> clazz.getSimpleName();
                    case Member otherMember -> otherMember.getName();
                    case null, default -> throw new UnsupportedOperationException();
                }
        );
    }

    public static String idOrDefault(AnnotatedElement element) {
        String className = objectName(element);
        Registered registered = element.getAnnotation(Registered.class);
        return registered == null || registered.value().isBlank()
                ? StringUtilities.snakeCase(className)
                : registered.value();
    }

    public static <T> Class<T> tryFindSuperclass(Set<Class<?>> classes, Class<?> child) {
        for (Class<?> superclass : classes) {
            if (superclass.isAssignableFrom(child)) {
                //Checked via isAssignableFrom
                //noinspection unchecked
                return (Class<T>) superclass;
            }
        }

        return null;
    }

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
