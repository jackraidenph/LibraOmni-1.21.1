package dev.jackraidenph.libraomni.data.proxy.compile;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import java.util.Map;

public class SyntheticAnnotationMirror implements AnnotationMirror {

    private final DeclaredType declaredType;
    private final Map<ExecutableElement, AnnotationValue> values;

    public SyntheticAnnotationMirror(@Nonnull DeclaredType declaredType, @NotNull Map<ExecutableElement, AnnotationValue> values) {
        this.declaredType = declaredType;
        this.values = values;
    }

    @Override
    public @NotNull DeclaredType getAnnotationType() {
        return declaredType;
    }

    @Override
    public @NotNull Map<ExecutableElement, AnnotationValue> getElementValues() {
        return values;
    }

    @Override
    public String toString() {
        return getAnnotationType() + "(" + getElementValues() + ")";
    }
}
