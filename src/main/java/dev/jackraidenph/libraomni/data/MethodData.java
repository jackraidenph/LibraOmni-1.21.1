package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.reflect.Method;
import java.util.List;

public record MethodData(String name, ClassData parent,
                         ClassData... paramTypes) implements AnnotatedReflectionData<Method> {
    public MethodData(ExecutableElement element, Elements elementUtils) {
        this(
                element.getSimpleName().toString(),
                new ClassData((TypeElement) element.getEnclosingElement(), elementUtils),
                paramsFromElement(element, elementUtils)
        );
    }

    private static ClassData[] paramsFromElement(ExecutableElement e, Elements elementUtils) {
        List<? extends VariableElement> l = e.getParameters();
        ClassData[] paramsArray = new ClassData[l.size()];
        for (int i = 0; i < l.size(); i++) {
            VariableElement variableElement = l.get(i);
            TypeMirror typeMirror = variableElement.asType();
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            paramsArray[i] = new ClassData(typeElement, elementUtils);
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
