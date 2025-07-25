package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.annotation.Registered;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Instead of throwing, methods return null.
 * If throwing an exception is actually needed - null check should be used on the calling side
 */
public class SafeReflectionUtil {

    public static Class<?>[] inferTypes(Object... objects) {
        Class<?>[] typesArray = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            typesArray[i] = objects[i].getClass();
        }

        return typesArray;
    }

    public static Class<?> selfOrAnnotationType(Object obj) {
        return (obj instanceof Annotation annotation) ? annotation.annotationType() : obj.getClass();
    }

    private static boolean secondTargetFitsFirst(Target first, Target second) {
        if (second.value().length == 0) {
            return true;
        }
        Set<ElementType> firstSet = Set.of(first.value());
        Set<ElementType> secondSet = Set.of(second.value());
        return firstSet.containsAll(secondSet) || secondSet.containsAll(firstSet);
    }

    private static boolean secondRetentionFitsFirst(Retention first, Retention second) {
        RetentionPolicy secondPolicy = second.value();
        return switch (first.value()) {
            case SOURCE -> true;
            case CLASS -> !secondPolicy.equals(RetentionPolicy.SOURCE);
            case RUNTIME -> secondPolicy.equals(RetentionPolicy.RUNTIME);
        };
    }

    public static boolean sameRetentionAndTarget(Annotation parent, Annotation meta) {
        Class<? extends Annotation> firstType = parent.annotationType();
        Class<? extends Annotation> secondType = meta.annotationType();

        Retention firstRetention = firstType.getAnnotation(Retention.class);
        Target firstTarget = firstType.getAnnotation(Target.class);

        Retention secondRetention = secondType.getAnnotation(Retention.class);
        Target secondTarget = secondType.getAnnotation(Target.class);

        return (firstRetention != null && firstTarget != null)
                && secondRetentionFitsFirst(firstRetention, secondRetention)
                && secondTargetFitsFirst(firstTarget, secondTarget);
    }

    public static Type[] extractTypeArguments(AnnotatedElement element) {
        if (!isExecutable(element)) {
            return null;
        }

        if (element instanceof Field field) {
            return extractTypeArgumentsFromFunctionalField(field);
        }

        if (element instanceof Executable executable) {
            return executable.getParameterTypes();
        }

        return null;
    }

    public static boolean isExecutable(AnnotatedElement element) {
        return (element instanceof Method)
                || (element instanceof Constructor<?>)
                || ((element instanceof Field) && isFunctionalInterface(selfOrReturnType(element)));
    }

    public static Type[] extractTypeArgumentsFromFunctionalField(Field field) {
        Class<?> type = field.getType();
        Method function = getAbstractMethods(type).getFirst();
        return function.getParameterTypes();
    }

    public static Map<String, Type> mapTypeParametersToArguments(ParameterizedType parameterizedType, Class<?> genericClass) {
        Map<String, Type> map = new HashMap<>();

        int i = 0;
        for (TypeVariable<?> typeVariable : genericClass.getTypeParameters()) {
            map.put(typeVariable.getName(), parameterizedType.getActualTypeArguments()[i]);
            i++;
        }

        return map;
    }

    public static Class<?> tryResolveFunctionalReturnType(Type type) {
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return getAbstractMethods((Class<?>) type).getFirst().getReturnType();
        }

        Class<?> clazz = (Class<?>) parameterizedType.getRawType();

        Method m = getAbstractMethods(clazz).getFirst();
        Type returnType = m.getGenericReturnType();

        return (Class<?>) mapTypeParametersToArguments(parameterizedType, clazz).get(returnType.getTypeName());
    }

    public static List<Method> getAbstractMethods(AnnotatedElement e) {
        if (!(e instanceof Class<?> clazz)) {
            throw new IllegalArgumentException();
        }

        return Arrays.stream(clazz.getMethods()).filter(m -> Modifier.isAbstract(m.getModifiers())).toList();
    }

    public static boolean isFunctionalInterface(AnnotatedElement element) {
        if (!(element instanceof Class<?> clazz)) {
            return false;
        }

        if (!clazz.isInterface()) {
            return false;
        }

        return getAbstractMethods(element).size() == 1;
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
                yield tryResolveFunctionalReturnType(field.getGenericType());
            }
            case Method method -> method.getReturnType();
            case null, default -> throw new UnsupportedOperationException(
                    "Trying to resolve type from [%s] (Element %s)"
                            .formatted(element == null ? null : element.getClass(), element)
            );
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
                    case null, default ->
                            throw new UnsupportedOperationException("Can't derive name from [%s] (Element %s)]"
                                    .formatted(element == null ? null : element.getClass().getName(), element));
                }
        );
    }

    public static String idOrDefault(AnnotatedElement element) {
        if (element instanceof DeferredHolder<?, ?> holder) {
            return holder.getId().getPath();
        }

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
