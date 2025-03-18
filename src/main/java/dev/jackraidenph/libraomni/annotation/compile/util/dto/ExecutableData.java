package dev.jackraidenph.libraomni.annotation.compile.util.dto;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public record ExecutableData(String name, TypeData parent, TypeData... paramTypes) {

    public ExecutableData(ExecutableElement element) {
        this(
                element.getSimpleName().toString(),
                new TypeData((TypeElement) element.getEnclosingElement()),
                paramsFromElement(element)
        );
    }

    public ExecutableData(TypeData parent, TypeData... paramTypes) {
        this("<init>", parent, paramTypes);
    }

    private static TypeData[] paramsFromElement(ExecutableElement e) {
        List<? extends VariableElement> l = e.getParameters();
        TypeData[] paramsArray = new TypeData[l.size()];
        for (int i = 0; i < l.size(); i++) {
            paramsArray[i] = new TypeData((TypeElement) l.get(i));
        }

        return paramsArray;
    }

    private static Class<?>[] paramsFromData(TypeData... data) {
        Class<?>[] classes = new Class[data.length];
        for (int i = 0; i < data.length; i++) {
            classes[i] = data[i].asClass();
        }

        return classes;
    }

    public Method asMethod() {
        try {
            return this.parent().asClass().getDeclaredMethod(this.name(), paramsFromData(this.paramTypes));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    public Constructor<?> asConstructor() {
        try {
            return this.parent().asClass().getDeclaredConstructor(paramsFromData(this.paramTypes));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
