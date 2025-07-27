package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.Constructor;
import java.util.List;

public record ConstructorData(ClassData parent,
                              ClassData... paramTypes) implements AnnotatedReflectionData<Constructor<?>> {
    public ConstructorData(ExecutableElement element, Elements elementUtils) {
        this(
                new ClassData((TypeElement) element.getEnclosingElement(), elementUtils),
                paramsFromElement(element, elementUtils)
        );
    }

    private static ClassData[] paramsFromElement(ExecutableElement e, Elements elementUtils) {
        List<? extends VariableElement> l = e.getParameters();
        ClassData[] paramsArray = new ClassData[l.size()];
        for (int i = 0; i < l.size(); i++) {
            paramsArray[i] = new ClassData((TypeElement) l.get(i), elementUtils);
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
    public Constructor<?> construct() {
        try {
            return this.parent().construct().getDeclaredConstructor(paramsFromData(this.paramTypes));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
