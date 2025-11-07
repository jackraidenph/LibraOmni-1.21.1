package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Repeatable;
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

    @Nonnull
    public static TypeMirror mirrorClass(Supplier<Class<?>> supplier) {
        try {
            supplier.get();
            throw new IllegalStateException("Method called in inappropriate context");
        } catch (MirroredTypeException typeException) {
            return typeException.getTypeMirror();
        }
    }

    @Nonnull
    public static List<? extends TypeMirror> mirrorClassArray(Supplier<Class<?>[]> supplier) {
        try {
            supplier.get();
            throw new IllegalStateException("Method called in inappropriate context");
        } catch (MirroredTypesException typeException) {
            return typeException.getTypeMirrors();
        }
    }

    public static boolean compareWithClass(AnnotationMirror annotationMirror, Class<?> clazz, Elements elements) {
        return elements.getBinaryName(toTypeElement(annotationMirror)).contentEquals(clazz.getName());
    }

    //Check if the annotation is a container for @Repeatable annotations specified as in https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    public static boolean isRepeatableContainer(AnnotationMirror mirror) {
        Object attributeValue = AnnotationMirrorUtil.getElementValue(mirror, "value");
        //Must be an array of annotations
        if (!(attributeValue instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        if (!(list.getFirst() instanceof AnnotationMirror inContainerMirror)) {
            return false;
        }

        TypeElement type = AnnotationMirrorUtil.toTypeElement(inContainerMirror);
        AnnotationMirror repeatableMirror = AnnotationMirrorUtil.findAnnotationMirror(type::getAnnotationMirrors, Repeatable.class.getName());
        if (repeatableMirror == null) {
            return false;
        }
        TypeMirror inRepeatableMirror = (TypeMirror) AnnotationMirrorUtil.getElementValue(repeatableMirror, "value");
        if (inRepeatableMirror == null) {
            return false;
        }
        return inRepeatableMirror.equals(mirror.getAnnotationType());
    }

    public static boolean isOnlyDirect(Elements elements, AnnotationMirror mirror) {
        return ProxyFactory.ONLY_DIRECT.stream().anyMatch(c -> AnnotationMirrorUtil.compareWithClass(mirror, c, elements));
    }

    public static List<AnnotationMirror> unwrapRepeatableContainer(AnnotationMirror annotation) {
        if (!isRepeatableContainer(annotation)) {
            throw new IllegalArgumentException("Not a container for @Repeatable");
        }
        try {
            AnnotationValue value = annotation.getElementValues()
                    .entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Repeatable container doesn't contain 'value' attribute"))
                    .getValue();
            Object obj = value.getValue();
            if (!(obj instanceof List<?> arr)) {
                throw new IllegalArgumentException("Can't unwrap non-array");
            }
            if (arr.isEmpty()) {
                return List.of();
            }
            //noinspection unchecked
            return (List<AnnotationMirror>) arr;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Not an aray of AnnotationMirrors", e);
        }
    }
}
