package dev.jackraidenph.libraomni.annotation.compile.util;

import dev.jackraidenph.libraomni.LibraOmni;

import javax.lang.model.element.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringJoiner;

public class SerializationHelper {

    private SerializationHelper() {
    }

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

    private static Class<?> classOrPrimitive(String name) {
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

    public static String toClassString(TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
    }

    public static Class<?> toClass(String typeElementString) {
        return classOrPrimitive(typeElementString);
    }

    public static String classesToString(Class<?>[] classes) {
        StringJoiner paramTypes = new StringJoiner(",", "(", ")");
        for (Class<?> param : classes) {
            paramTypes.add(param.getName());
        }
        return paramTypes.toString();
    }

    public static String getParameterTypesString(ExecutableElement executableElement) {
        StringJoiner paramTypes = new StringJoiner(",", "(", ")");
        for (VariableElement param : executableElement.getParameters()) {
            paramTypes.add(param.asType().toString());
        }
        return paramTypes.toString();
    }

    public static String toMethodString(ExecutableElement executableElement) {
        if (executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
            throw new IllegalArgumentException("Passed element is a constructor");
        }

        String clazz = toClassString((TypeElement) executableElement.getEnclosingElement());
        String name = executableElement.getSimpleName().toString();
        String paramTypes = getParameterTypesString(executableElement);
        return clazz + "#" + name + paramTypes;
    }

    private static ExecutableData<?> parseExecutableData(String string) {
        String[] parts = string.split("#");
        String clazzString = parts[0];
        Class<?> clazz = toClass(clazzString);
        String[] nameParams = parts[1].split("\\(");
        String name = nameParams[0];
        String paramsString = nameParams[1].substring(0, nameParams[1].length() - 1);

        if (!paramsString.isBlank()) {
            String[] paramTypesStrings = paramsString.split(",");
            Class<?>[] paramTypes = new Class<?>[paramTypesStrings.length];
            for (int i = 0; i < paramTypesStrings.length; i++) {
                paramTypes[i] = toClass(paramTypesStrings[i]);
            }

            return new ExecutableData<>(clazz, name, paramTypes);
        }

        return new ExecutableData<>(clazz, name);
    }

    public static Method toMethod(String string) throws NoSuchMethodException {
        return parseExecutableData(string).asMethod();
    }

    public static String toConstructorString(ExecutableElement executableElement) {
        if (!executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
            throw new IllegalArgumentException("Passed element is not a constructor");
        }

        String clazz = toClassString((TypeElement) executableElement.getEnclosingElement());
        String paramTypes = getParameterTypesString(executableElement);
        return clazz + "#" + "<init>" + paramTypes;
    }

    public static Constructor<?> toConstructor(String string) throws NoSuchMethodException {
        return parseExecutableData(string).asConstructor();
    }

    public static String toFieldString(VariableElement variableElement) {
        String clazz = toClassString((TypeElement) variableElement.getEnclosingElement());
        String name = variableElement.getSimpleName().toString();
        return clazz + "#" + name;
    }

    public static Field toField(String string) throws NoSuchFieldException {
        String[] parts = string.split("#");
        String clazzString = parts[0];
        Class<?> clazz = toClass(clazzString);
        String name = parts[1];

        return clazz.getDeclaredField(name);
    }

    public static String getElementString(Element element) {
        return switch (element.getKind()) {
            case CLASS -> toClassString((TypeElement) element);
            case FIELD -> toFieldString((VariableElement) element);
            case CONSTRUCTOR -> toConstructorString((ExecutableElement) element);
            case METHOD -> toMethodString((ExecutableElement) element);
            default -> throw new IllegalStateException();
        };
    }

    public static Object parse(ElementKind elementKind, String string) throws NoSuchFieldException, NoSuchMethodException {
        return switch (elementKind) {
            case CLASS -> toClass(string);
            case FIELD -> toField(string);
            case CONSTRUCTOR -> toConstructor(string);
            case METHOD -> toMethod(string);
            default -> throw new UnsupportedOperationException();
        };
    }

    private record ExecutableData<T>(Class<T> clazz, String name, Class<?>... paramTypes) {
        public Constructor<T> asConstructor() throws NoSuchMethodException {
            return clazz.getDeclaredConstructor(paramTypes);
        }

        public Method asMethod() throws NoSuchMethodException {
            return clazz.getMethod(name, paramTypes);
        }

        @Override
        public String toString() {
            return clazz.getName() + "#" + name + classesToString(paramTypes);
        }
    }
}
