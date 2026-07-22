package dev.jackraidenph.libraomni.data.proxy.compile;

import dev.jackraidenph.libraomni.util.ElementUtil;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import java.util.*;
import java.util.Map.Entry;

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
        return stringIdentity(this, false);
    }

    public static String stringIdentity(AnnotationMirror mirror, boolean sort) {
        String type = ElementUtil.Javac.binaryName(mirror.getAnnotationType());
        StringJoiner argsJoiner = new StringJoiner(",", "(", ")");


        List<Entry<? extends ExecutableElement, ? extends AnnotationValue>> l = new ArrayList<>(mirror.getElementValues().entrySet());

        if (sort) {
            l.sort(Comparator.comparingInt(e -> e.getKey().getSimpleName().charAt(0)));
        }

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : l) {
            ExecutableElement k = e.getKey();
            AnnotationValue v = e.getValue();

            String name = k.getSimpleName().toString();
            Object value = v.getValue();
            if (value == null) {
                value = k.getDefaultValue();
            }

            if (value == null) {
                continue;
            }

            value = ElementUtil.tryConvertInternalRepresentation(k.getReturnType(), value);

            String valueStr = value.getClass().isArray() ? SafeReflectionUtil.arrayToString(value) : value.toString();

            argsJoiner.add(name + "=" + valueStr);
        }

        return type + argsJoiner;
    }
}
