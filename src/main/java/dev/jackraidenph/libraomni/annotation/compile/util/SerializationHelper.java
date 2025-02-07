package dev.jackraidenph.libraomni.annotation.compile.util;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.StringJoiner;

public class SerializationHelper {

    public static final SerializationHelper INSTANCE = new SerializationHelper(ReflectionCachingHelper.INSTANCE);

    private final ReflectionCachingHelper reflectionCachingHelper;

    private SerializationHelper(ReflectionCachingHelper reflectionCachingHelper) {
        this.reflectionCachingHelper = reflectionCachingHelper;
    }

    public String toClassString(TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
    }

    public Class<?> toClass(String typeElementString) {
        return this.reflectionCachingHelper.getClassOrPrimitiveByName(typeElementString);
    }

    public static String classesToString(Class<?>[] classes) {
        StringJoiner paramTypes = new StringJoiner(",", "(", ")");
        for (Class<?> param : classes) {
            paramTypes.add(param.getName());
        }
        return paramTypes.toString();
    }

    public String getParameterTypesString(ExecutableElement executableElement) {
        StringJoiner paramTypes = new StringJoiner(",", "(", ")");
        for (VariableElement param : executableElement.getParameters()) {
            paramTypes.add(param.asType().toString());
        }
        return paramTypes.toString();
    }

    public String toMethodString(ExecutableElement executableElement) {
        if (executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
            throw new IllegalArgumentException("Passed element is a constructor");
        }

        String clazz = this.toClassString((TypeElement) executableElement.getEnclosingElement());
        String name = executableElement.getSimpleName().toString();
        String paramTypes = this.getParameterTypesString(executableElement);
        return clazz + "#" + name + paramTypes;
    }

    private record MethodData(Class<?> clazz, String name, Class<?>[] paramTypes) {

    }

    private MethodData toMethodData(String string) {
        String[] parts = string.split("#");
        String clazzString = parts[0];
        Class<?> clazz = this.toClass(clazzString);
        String[] nameParams = parts[1].split("\\(");
        String name = nameParams[0];
        String paramsString = nameParams[1].substring(0, nameParams[1].length() - 1);

        if (!paramsString.isBlank()) {
            String[] paramTypesStrings = paramsString.split(",");
            Class<?>[] paramTypes = new Class<?>[paramTypesStrings.length];
            for (int i = 0; i < paramTypesStrings.length; i++) {
                paramTypes[i] = this.toClass(paramTypesStrings[i]);
            }

            return new MethodData(clazz, name, paramTypes);
        } else {
            return new MethodData(clazz, name, new Class[]{});
        }
    }

    public Method toMethod(String string) {
        MethodData methodData = this.toMethodData(string);
        return this.reflectionCachingHelper.getDeclaredMethod(
                methodData.clazz(),
                methodData.name(),
                methodData.paramTypes()
        );
    }

    public String toConstructorString(ExecutableElement executableElement) {
        if (!executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
            throw new IllegalArgumentException("Passed element is not a constructor");
        }

        String clazz = this.toClassString((TypeElement) executableElement.getEnclosingElement());
        String paramTypes = this.getParameterTypesString(executableElement);
        return clazz + "#" + "<init>" + paramTypes;
    }

    public Constructor<?> toConstructor(String string) {
        MethodData methodData = toMethodData(string);
        return this.reflectionCachingHelper.getDeclaredConstructor(
                methodData.clazz(),
                methodData.paramTypes()
        );
    }

    public String toFieldString(VariableElement variableElement) {
        String clazz = this.toClassString((TypeElement) variableElement.getEnclosingElement());
        String name = variableElement.getSimpleName().toString();
        return clazz + "#" + name;
    }

    public Field toField(String string) {
        String[] parts = string.split("#");
        String clazzString = parts[0];
        Class<?> clazz = this.toClass(clazzString);
        String name = parts[1];

        return this.reflectionCachingHelper.getDeclaredField(clazz, name);
    }
}
