package dev.jackraidenph.libraomni.common;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class AnnotationMirrorUtil {

    public static Object getElementValue(AnnotationMirror mirror, String name) {
        AnnotationValue value = mirror.getElementValues()
                .entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals(name))
                .map(Entry::getValue)
                .findFirst()
                .orElse(null);
        if (value == null) {
            return null;
        }
        return value.getValue();
    }

    public static AnnotationMirror findAnnotationMirror(Supplier<List<? extends AnnotationMirror>> mirrors, String qualifiedName) {
        return mirrors.get().stream()
                .filter(mirror -> ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(qualifiedName))
                .findFirst()
                .orElse(null);
    }

    public static TypeElement toTypeElement(AnnotationMirror mirror) {
        return (TypeElement) mirror.getAnnotationType().asElement();
    }

    public static boolean compareWithClass(AnnotationMirror annotationMirror, Class<?> clazz, Elements elements) {
        return elements.getBinaryName(toTypeElement(annotationMirror)).contentEquals(clazz.getName());
    }

}
