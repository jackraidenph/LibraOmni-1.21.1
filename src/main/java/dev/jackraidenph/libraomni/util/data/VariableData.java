package dev.jackraidenph.libraomni.util.data;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Field;

public record VariableData(String name, TypeData parent) {
    public VariableData(VariableElement variableElement) {
        this(
                variableElement.getSimpleName().toString(),
                new TypeData((TypeElement) variableElement.getEnclosingElement())
        );
    }

    public Field asField() {
        try {
            return this.parent().asClass().getDeclaredField(this.name());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
