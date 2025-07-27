package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.Field;

public record FieldData(String name, ClassData parent) implements AnnotatedReflectionData<Field> {
    public FieldData(VariableElement variableElement, Elements elementUtils) {
        this(
                variableElement.getSimpleName().toString(),
                new ClassData((TypeElement) variableElement.getEnclosingElement(), elementUtils)
        );
    }

    @Override
    public ElementKind kind() {
        return ElementKind.FIELD;
    }

    @Override
    public Field construct() {
        try {
            return this.parent().construct().getDeclaredField(this.name());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
