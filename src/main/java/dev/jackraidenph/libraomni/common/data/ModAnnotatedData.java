package dev.jackraidenph.libraomni.common.data;

import javax.lang.model.element.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ModAnnotatedData {

    private final Set<String> classes = new HashSet<>();
    private final Set<VariableData> fields = new HashSet<>();
    private final Set<ExecutableData> methods = new HashSet<>();
    private final Set<ExecutableData> constructors = new HashSet<>();

    public void addElement(Element element) {
        switch (element) {
            case TypeElement typeElement -> this.addClass(typeElement);
            case VariableElement variableElement -> this.addField(variableElement);
            case ExecutableElement executableElement -> {
                if (executableElement.getKind().equals(ElementKind.METHOD)) {
                    this.addMethod(executableElement);
                } else if (executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    this.addConstructor(executableElement);
                }

            }
            default -> throw new UnsupportedOperationException("Not supported");
        }
    }

    public void addClass(TypeElement typeElement) {
        this.classes.add(typeElement.getQualifiedName().toString());
    }

    public void addField(VariableElement variableElement) {
        this.fields.add(new VariableData(variableElement));
    }

    public void addMethod(ExecutableElement executableElement) {
        this.methods.add(new ExecutableData(executableElement));
    }

    public void addConstructor(ExecutableElement executableElement) {
        this.constructors.add(new ExecutableData(executableElement));
    }

    public Set<Class<?>> getClasses() {
        return classes.stream().map(TypeData::classOrPrimitive).collect(Collectors.toSet());
    }

    public Set<Field> getFields() {
        return fields.stream().map(VariableData::asField).collect(Collectors.toSet());
    }

    public Set<Method> getMethods() {
        return methods.stream().map(ExecutableData::asMethod).collect(Collectors.toSet());
    }

    public Set<Constructor<?>> getConstructors() {
        return constructors.stream().map(ExecutableData::asConstructor).collect(Collectors.toSet());
    }

    public Set<AnnotatedElement> getElements() {
        Set<AnnotatedElement> elements = new HashSet<>();
        elements.addAll(this.getClasses());
        elements.addAll(this.getFields());
        elements.addAll(this.getMethods());
        elements.addAll(this.getConstructors());

        return elements;
    }
}
