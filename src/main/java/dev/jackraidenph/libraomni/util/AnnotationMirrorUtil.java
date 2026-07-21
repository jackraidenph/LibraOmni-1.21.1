package dev.jackraidenph.libraomni.util;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import dev.jackraidenph.libraomni.data.proxy.runtime.SyntheticAnnotation;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * A utility class with hellper methods to work with and ONLY with AnnotationMirrors
 */
public final class AnnotationMirrorUtil {

    private AnnotationMirrorUtil() {

    }

    public static Annotation tryCovnertToAnnotation(AnnotationMirror mirror) {
        TypeElement type = AnnotationMirrorUtil.toTypeElement(mirror);
        String binaryName = ElementUtil.Javac.binaryName(type);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(binaryName, Annotation.class);

        if (clazz == null || ElementUtil.isUnfoldUnsupported(type)) {
            return null;
        }

        Map<String, Object> reflectiveReplacements = new HashMap<>();

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> kv : mirror.getElementValues().entrySet()) {
            String attribute = kv.getKey().getSimpleName().toString();
            Object value = kv.getValue().getValue();
            value = ElementUtil.tryConvertInternalRepresentation(kv.getKey().getReturnType(), value);

            reflectiveReplacements.put(attribute, value);
        }

        return SyntheticAnnotation.create(clazz, reflectiveReplacements);
    }

    public static ExecutableElement getExecutableElementByName(AnnotationMirror mirror, String name) {
        return Javac.getElementValuesWithDefaults(mirror)
                .keySet().stream()
                .filter(executableElement -> executableElement.getSimpleName().contentEquals(name))
                .findFirst()
                .orElse(null);
    }

    public static AnnotationValue getElementValue(AnnotationMirror mirror, String name) {
        return AnnotationMirrorUtil.Javac.getElementValuesWithDefaults(mirror)
                .entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals(name))
                .map(Entry::getValue)
                .findFirst()
                .orElse(null);
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

    public static boolean compareWithClass(AnnotationMirror annotationMirror, Class<?> clazz) {
        return ElementUtil.Javac.binaryName(toTypeElement(annotationMirror)).contentEquals(clazz.getName());
    }

    //Check if the annotation is a container for @Repeatable annotations specified as in https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    public static boolean isRepeatableContainer(AnnotationMirror mirror) {
        AnnotationValue annotationValue = AnnotationMirrorUtil.getElementValue(mirror, "value");
        if (annotationValue == null) {
            return false;
        }
        Object attributeValue = annotationValue.getValue();
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
        AnnotationValue mirrorAnnotationValue = AnnotationMirrorUtil.getElementValue(repeatableMirror, "value");
        if (mirrorAnnotationValue == null) {
            return false;
        }
        Object inRepeatableMirror = mirrorAnnotationValue.getValue();
        return inRepeatableMirror.equals(mirror.getAnnotationType());
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

    public static class Javac {
        //STATIC REIMPL of JavacElements#getElementValuesWithDefaults
        public static Map<ExecutableElement, AnnotationValue> getElementValuesWithDefaults(AnnotationMirror a) {
            DeclaredType annotype = a.getAnnotationType();

            Map<ExecutableElement, AnnotationValue> res = new HashMap<>();

            List<? extends Element> enclosed = annotype.asElement().getEnclosedElements();
            for (ExecutableElement ex : ElementFilter.methodsIn(enclosed)) {
                MethodSymbol meth = (MethodSymbol) ex;
                Attribute defaultValue = meth.getDefaultValue();
                if (defaultValue != null && !res.containsKey(meth)) {
                    res.put(meth, defaultValue);
                }
            }

            Map<? extends ExecutableElement, ? extends AnnotationValue> existing = a.getElementValues();
            res.putAll(existing);

            return res;
        }
    }
}
