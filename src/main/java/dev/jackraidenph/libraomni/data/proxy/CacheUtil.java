package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.common.*;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CacheUtil {

    public static Map<Class<?>, Map<String, Object>> getReplacementValues(AnnotatedElement origin, Annotation annotation) {
        Map<Class<?>, Map<String, Object>> replacements = new HashMap<>();

        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Replaces replacementInfo = method.getAnnotation(Replaces.class);
            if (replacementInfo == null) {
                continue;
            }

            Object value = UnsafeReflectionUtil.getMethodValue(method, annotation);

            if (value == null) {
                continue;
            }

            Method targetMethod;
            try {
                targetMethod = replacementInfo.in().getDeclaredMethod(replacementInfo.attribute());
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Attribute [%s] doesn't exist in annotation [%s]".formatted(replacementInfo.attribute(), replacementInfo.in()));
            }

            if (value instanceof String str) {
                value = TransformerUtil.replacePlaceholders(origin, str);
            }

            value = TransformerUtil.tryTransform(value, replacementInfo, targetMethod);

            Class<?> target = replacementInfo.in();
            replacements.computeIfAbsent(target, v -> new HashMap<>()).put(replacementInfo.attribute(), value);
        }

        return replacements;
    }

    //Keys are TypeElements' binary names, because, unlike classes, TypeElements somehow don't work as Map keys
    public static Map<String, Map<ExecutableElement, AnnotationValue>> getReplacementValues(AnnotatedConstruct origin, AnnotationMirror annotationMirror) {
        Map<String, Map<ExecutableElement, AnnotationValue>> replacements = new HashMap<>();

        Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues = AnnotationMirrorUtil.Javac.getElementValuesWithDefaults(annotationMirror);

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : annotationValues.entrySet()) {
            ExecutableElement attribute = e.getKey();
            AnnotationValue unwrapped = e.getValue();

            Replaces replacementInfo = attribute.getAnnotation(Replaces.class);
            if (replacementInfo == null) {
                continue;
            }

            TypeMirror target = ElementUtil.mirrorClass(replacementInfo::in);

            ExecutableElement targetAttributeElement = ElementUtil.getExecutableElementByName(
                    replacementInfo.attribute(),
                    ElementUtil.mirrorToElement(target)
            );

            if (targetAttributeElement == null) {
                throw new IllegalArgumentException("Attribute [%s] doesn't exist in annotation [%s]".formatted(replacementInfo.attribute(), target));
            }

            Object oldValue = unwrapped.getValue();
            if (oldValue instanceof String str) {
                oldValue = TransformerUtil.replacePlaceholders(origin, str);
            }

            Object newValue = TransformerUtil.tryTransform(oldValue, replacementInfo, targetAttributeElement);
            unwrapped = new AnnotationValueWrapper(newValue, unwrapped);

            TypeElement element = ElementUtil.mirrorToElement(target);
            var map = replacements.computeIfAbsent(ElementUtil.Javac.binaryName(element), v -> new HashMap<>());
            map.put(targetAttributeElement, unwrapped);
        }

        return replacements;
    }

    private record AnnotationValueWrapper(Object value, AnnotationValue parent) implements AnnotationValue {

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
}
