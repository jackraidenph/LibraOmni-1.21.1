package dev.jackraidenph.libraomni.data.proxy.compile;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;

public record AnnotationValueWrapper(Object value, AnnotationValue parent) implements AnnotationValue {

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
        return parent.accept(v, p);
    }

    @Override
    public @NotNull String toString() {
        return String.valueOf(value);
    }
}
