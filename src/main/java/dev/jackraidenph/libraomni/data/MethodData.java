package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Method;
import java.util.List;

public record MethodData(String name, ClassData parent, ClassData... paramTypes) implements AnnotatedReflectionData<Method> {
    public MethodData(ExecutableElement element) {
        this(
                element.getSimpleName().toString(),
                new ClassData((TypeElement) element.getEnclosingElement()),
                paramsFromElement(element)
        );
    }

    private static ClassData[] paramsFromElement(ExecutableElement e) {
        List<? extends VariableElement> l = e.getParameters();
        ClassData[] paramsArray = new ClassData[l.size()];
        for (int i = 0; i < l.size(); i++) {
            paramsArray[i] = new ClassData((TypeElement) l.get(i));
        }

        return paramsArray;
    }

    private static Class<?>[] paramsFromData(ClassData... data) {
        Class<?>[] classes = new Class[data.length];
        for (int i = 0; i < data.length; i++) {
            classes[i] = data[i].construct();
        }

        return classes;
    }

    @Override
    public ElementKind kind() {
        return ElementKind.METHOD;
    }

    @Override
    public Method construct() {
        try {
            return this.parent().construct().getDeclaredMethod(this.name(), paramsFromData(this.paramTypes));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
