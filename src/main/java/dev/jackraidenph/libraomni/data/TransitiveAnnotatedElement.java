package dev.jackraidenph.libraomni.data;

import dev.jackraidenph.libraomni.annotation.Composite;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransitiveAnnotatedElement implements AnnotatedElement {

    private final AnnotatedElement annotatedElement;
    private final Annotation[] allAnnotations;
    private final Annotation[] allDeclaredAnnotations;

    public TransitiveAnnotatedElement(AnnotatedElement annotatedElement) {
        this.annotatedElement = annotatedElement;
        final List<Annotation> all = new ArrayList<>();
        recursiveGatherTransitive(getDirectAnnotations(), all);
        allAnnotations = all.toArray(Annotation[]::new);

        final List<Annotation> declared = new ArrayList<>();
        recursiveGatherTransitive(getDeclaredDirectAnnotations(), declared);
        allDeclaredAnnotations = declared.toArray(Annotation[]::new);
    }

    public AnnotatedElement unwrap() {
        return annotatedElement;
    }

    private boolean applicableToElement(Annotation annotation) {
        Target targetAnnotation = annotation.annotationType().getAnnotation(Target.class);
        if (targetAnnotation == null) {
            return false;
        }

        final ElementType annotatedElementType = SafeReflectionUtil.getElementType(annotatedElement);

        if (annotatedElementType == null) {
            return false;
        }

        ElementType[] types = targetAnnotation.value();
        for (ElementType t : types) {
            if (annotatedElementType.equals(t)) {
                return true;
            }
        }

        return false;
    }

    private void recursiveGatherTransitiveStep(Annotation annotation, List<Annotation> out) {
        if (applicableToElement(annotation)) {
            out.add(annotation);
        }
        Composite composite = annotation.annotationType().getAnnotation(Composite.class);
        if (composite != null) {
            for (Annotation transitive : annotation.annotationType().getAnnotations()) {
                if (!transitive.equals(composite)) {
                    recursiveGatherTransitiveStep(transitive, out);
                }
            }
        }
    }

    private void recursiveGatherTransitive(List<Annotation> startList, List<Annotation> outList) {
        for (Annotation annotation : startList) {
            recursiveGatherTransitiveStep(annotation, outList);
        }
    }

    private List<Annotation> getDirectAnnotations() {
        return Arrays.asList(annotatedElement.getAnnotations());
    }

    private List<Annotation> getDeclaredDirectAnnotations() {
        return Arrays.asList(annotatedElement.getDeclaredAnnotations());
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        for (Annotation annotation : getAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return Arrays.copyOf(allAnnotations, allAnnotations.length);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return Arrays.copyOf(allDeclaredAnnotations, allDeclaredAnnotations.length);
    }

    @Override
    public String toString() {
        return this.unwrap().toString();
    }
}
