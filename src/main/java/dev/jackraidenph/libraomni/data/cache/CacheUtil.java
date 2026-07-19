package dev.jackraidenph.libraomni.data.cache;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.common.*;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CacheUtil {

    public static Map<Class<?>, Map<String, Object>> getReplacementValues(Annotation annotation) {
        Map<Class<?>, Map<String, Object>> replacements = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Replaces replacer = method.getAnnotation(Replaces.class);
            if (replacer == null) {
                continue;
            }

            Object value = UnsafeReflectionUtil.getMethodValue(method, annotation);

            if (value == null) {
                continue;
            }

            Method targetMethod;
            try {
                targetMethod = replacer.in().getDeclaredMethod(replacer.attribute());
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Attribute [%s] doesn't exist in annotation [%s]".formatted(replacer.attribute(), replacer.in()));
            }

            value = TransformerUtil.tryTransform(value, replacer, targetMethod);

            Class<?> target = replacer.in();
            replacements.computeIfAbsent(target, v -> new HashMap<>()).put(replacer.attribute(), value);
        }

        return replacements;
    }

    //Keys are TypeElements' binary names, because, unlike classes, TypeElements somehow don't works as Map keys
    public static Map<String, Map<ExecutableElement, AnnotationValue>> getReplacementValues(AnnotationMirror annotationMirror) {
        Map<String, Map<ExecutableElement, AnnotationValue>> replacements = new HashMap<>();

        Map<? extends ExecutableElement, ? extends AnnotationValue> valueMap = AnnotationMirrorUtil.Javac.getElementValuesWithDefaults(annotationMirror);

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : valueMap.entrySet()) {
            ExecutableElement attribute = e.getKey();
            AnnotationValue unwrapped = e.getValue();

            Replaces replacer = attribute.getAnnotation(Replaces.class);
            if (replacer == null) {
                continue;
            }

            TypeMirror target = ElementUtil.mirrorClass(replacer::in);

            ExecutableElement targetAttributeElement = ElementUtil.getExecutableElementByName(
                    replacer.attribute(),
                    ElementUtil.mirrorToElement(target)
            );

            if (targetAttributeElement == null) {
                throw new IllegalArgumentException("Attribute [%s] doesn't exist in annotation [%s]".formatted(replacer.attribute(), target));
            }

            Object newValue = TransformerUtil.tryTransform(unwrapped.getValue(), replacer, targetAttributeElement);
            unwrapped = new AnnotationValueWrapper(newValue, unwrapped);

            TypeElement element = ElementUtil.mirrorToElement(target);
            var map = replacements.computeIfAbsent(ElementUtil.Javac.binaryName(element), v -> new HashMap<>());
            map.put(targetAttributeElement, unwrapped);
        }

        return replacements;
    }

    public static boolean isUnfoldUnsupported(TypeElement type) {
        return AnnotationProcessorConstants.UNFOLD_UNSUPPORTED.stream()
                .anyMatch(c -> ElementUtil.Javac.binaryName(type).equals(c.getName()));
    }

    public static Object normalizeValue(Object internal) {
        if (internal instanceof com.sun.tools.javac.util.List<?> sunList) {
            return sunListToArray(sunList);
        }

        if (internal instanceof Symbol.VarSymbol varSymbol) {
            if (varSymbol.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                return varSymbolToEnum(varSymbol);
            } else {
                throw new UnsupportedOperationException("Ecountered VarSymbol of kind [%s]".formatted(varSymbol.getKind()));
            }
        }

        return internal;
    }

    private static Object varSymbolToEnum(Symbol.VarSymbol varSymbol) {
        String binary = varSymbol.owner.flatName().toString();
        var clazz = SafeReflectionUtil.forNameSubclass(binary, Enum.class);
        if (clazz == null) {
            throw new IllegalStateException("Failed to instantiate enum class for name [%s]".formatted(binary));
        }
        //noinspection unchecked
        return Enum.valueOf(clazz, varSymbol.name.toString());
    }

    private static Object sunListToArray(com.sun.tools.javac.util.List<?> sunList) {
        if (sunList.isEmpty()) {
            return new Object[0];
        }

        Attribute.Constant first = (Attribute.Constant) sunList.getFirst();

        String binary = first.type.tsym.flatName().toString();
        Class<?> clazz = SafeReflectionUtil.forName(binary);

        Object arr = Array.newInstance(clazz, sunList.size());
        for (int i = 0; i < sunList.size(); i++) {
            Array.set(arr, i, ((Attribute.Constant) sunList.get(i)).value);
        }

        return arr;
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
