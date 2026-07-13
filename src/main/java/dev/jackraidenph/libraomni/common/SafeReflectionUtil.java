package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.annotation.meta.Id;
import dev.jackraidenph.libraomni.data.proxy.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Instead of throwing, methods return null.
 * If throwing an exception is actually needed - null check should be used on the calling side
 */
public class SafeReflectionUtil {

    @SuppressWarnings("unchecked")
    public static <T> T[] genericArray(Class<T> clazz, int... dimensions) {
        return (T[]) Array.newInstance(clazz, dimensions);
    }

    public static Class<?>[] inferTypes(Object... objects) {
        if (objects == null) {
            return new Class[0];
        }

        Class<?>[] typesArray = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            typesArray[i] = objects[i].getClass();
        }

        return typesArray;
    }

    public static Object selfOrSingletonArray(Class<?> destinationClass, Object val) {
        Class<?> type = SafeReflectionUtil.selfOrAnnotationType(val);
        if (destinationClass.isArray() && !type.isArray() && destinationClass.componentType().isAssignableFrom(type)) {
            Object arr = Array.newInstance(type, 1);
            Array.set(arr, 0, val);
            return arr;
        } else {
            return val;
        }
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

    public static Type[] getMethodParameters(AnnotatedElement element) {
        if (element instanceof Field field) {
            return extractTypeArgumentsFromFunctionalField(field);
        }

        if (element instanceof Executable executable) {
            return executable.getParameterTypes();
        }

        return null;
    }

    public static Type[] extractTypeArguments(AnnotatedElement element) {
        if (element instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
        }

        if (element instanceof Field field && field.getGenericType() instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
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
        return idOrDefault(ProxyFactory.tryMakeAnnotatedElementProxy(element, null));
    }

    public static DeferredHolder<?, ?> tryCastToDeferredHolder(AnnotatedElement element) {
        if (element instanceof Field field
                && UnsafeReflectionUtil.getFieldValue(field, null, false) instanceof DeferredHolder<?, ?> deferredHolder) {
            return deferredHolder;
        }
        return null;
    }

    public static String holderId(AnnotatedElement e) {
        DeferredHolder<?, ?> holder = SafeReflectionUtil.tryCastToDeferredHolder(e);
        if (holder != null) {
            return holder.getId().getPath();
        }
        return null;
    }

    public static String id(ProxiedAnnotatedElement e) {
        AnnotatedElement object = e.proxiedElement();
        String holderId = holderId(object);
        if (holderId != null && !holderId.isBlank()) {
            return holderId;
        }

        Id id = e.getAnnotation(Id.class);
        if (id != null && id.value() != null) {
            //If @Id is actually present, but blank, use object's name
            if (id.value().isBlank()) {
                return StringUtilities.snakeCase(objectName(e.proxiedElement()));
            }
            return id.value();
        }

        return null;
    }

    public static String idOrDefault(ProxiedAnnotatedElement element) {
        String id = id(element);
        if (id != null && !id.isBlank()) {
            return id;
        }

        return StringUtilities.snakeCase(objectName(element.proxiedElement()));
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

    //Check if the annotation is a container for @Repeatable annotations specified as in https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    public static boolean isRepeatableContainer(Class<? extends Annotation> annotationType) {
        Method[] attributes = annotationType.getMethods();
        //Must contain "value" attribute
        Optional<Method> valueOptional = Arrays.stream(attributes).filter(m -> m.getName().equals("value")).findFirst();
        if (valueOptional.isEmpty()) {
            return false;
        }
        Method value = valueOptional.get();
        Class<?> returnType = value.getReturnType();
        //Must be an array of annotations
        if (!Annotation[].class.isAssignableFrom(returnType)) {
            return false;
        }
        //Array must contain annotations meta-annotated with @Repeatable
        Repeatable repeatable = (returnType.getComponentType()).getAnnotation(Repeatable.class);
        if (repeatable == null) {
            return false;
        }
        //Repeatable should specify the original annotation as a container
        return repeatable.value().equals(annotationType);
    }

    public static List<Annotation> unwrapRepeatableContainer(Annotation annotation) {
        if (!isRepeatableContainer(annotation.annotationType())) {
            throw new IllegalArgumentException("Not a container for @Repeatable");
        }
        try {
            Method value = annotation.annotationType().getMethod("value");
            return List.of(UnsafeReflectionUtil.getMethodValue(value, annotation));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
