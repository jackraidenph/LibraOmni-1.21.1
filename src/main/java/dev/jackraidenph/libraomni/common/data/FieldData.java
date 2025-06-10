package dev.jackraidenph.libraomni.common.data;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Field;

public record FieldData(String name, ClassData parent) implements AnnotatedReflectionData<Field> {
    public FieldData(VariableElement variableElement) {
        this(
                variableElement.getSimpleName().toString(),
                new ClassData((TypeElement) variableElement.getEnclosingElement())
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
