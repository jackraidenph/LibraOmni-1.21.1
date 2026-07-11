package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.Replaces.NoOpTransformer;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttributeReplacements {

    private final Map<String, Replacement> attributeReplacements = new HashMap<>();
    private final String parentAnnotationBinaryName; //For unity between runtime and compile time, store just the binary name

    public AttributeReplacements(String parentAnnotationBinaryName) {
        this.parentAnnotationBinaryName = parentAnnotationBinaryName;
    }

    public Set<String> getNonCommonMethods(Annotation other) {
        Set<String> otherMethods = Arrays.stream(other.annotationType().getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        Set<String> delegatedMethods = attributeReplacements.values().stream()
                .map(Replacement::annotation)
                .map(Replaces::attribute).collect(Collectors.toSet());

        delegatedMethods.removeAll(otherMethods);
        return Collections.unmodifiableSet(delegatedMethods);
    }

    public String getParentAnotationBinaryName() {
        return parentAnnotationBinaryName;
    }

    public boolean isEmpty() {
        return attributeReplacements.isEmpty();
    }

    public boolean hasReplacementFor(String attributeName) {
        return attributeReplacements.containsKey(attributeName);
    }

    public Object getReplacementValue(String attributeName) {
        Replacement entry = attributeReplacements.get(attributeName);
        if (entry == null) {
            return null;
        }
        return entry.replacementValue();
    }

    public Replaces getReplacementAnnotation(String attributeName) {
        Replacement entry = attributeReplacements.get(attributeName);
        if (entry == null) {
            return null;
        }
        return entry.annotation();
    }

    private static Class<?> tryGetClass(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        Class<?> clazz = SafeReflectionUtil.forName(name);
        //Might be an inner class
        if (clazz == null) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot < 0) {
                return clazz;
            }
            name = name.substring(0, lastDot) + '$' + name.substring(lastDot + 1);
            clazz = SafeReflectionUtil.forName(name);
        }
        return clazz;
    }

    private Class<? extends Function<Object, Object>> getTransformerClass(String attributeName) {
        Replaces annotation = getReplacementAnnotation(attributeName);
        if (annotation == null) {
            throw new IllegalStateException("@%s annotation not found for attribute [%s]".formatted(Replaces.class.getSimpleName(), attributeName));
        }

        Class<? extends Function<Object, Object>> transformerClazz;
        try {
            transformerClazz = annotation.transformer();
            return transformerClazz;
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            //noinspection unchecked
            transformerClazz = (Class<? extends Function<Object, Object>>) tryGetClass(typeMirror);
            if (transformerClazz == null) {
                throw new IllegalArgumentException("""
                    Transformer class [%s] not found.
                    Most probably, you are trying to use custom transformer implementation
                    for a compile-time annotation.
                    This won't work, your transformer is not yet compiled.
                    """.formatted(typeMirror.toString()));
            }
            return transformerClazz;
        }
    }

    private Function<Object, Object> constructTransformerInstance(String attributeName) {
        Class<? extends Function<Object, Object>> trasnformerClass = getTransformerClass(attributeName);
        return UnsafeReflectionUtil.tryConstruct(trasnformerClass);
    }

    public Object getTransformedReplacementValue(String attributeName) {
        Object oldVal = getReplacementValue(attributeName);
        if (oldVal == null) {
            return null;
        }

        String transformerName = getTransformerClass(attributeName).getName();
        if (transformerName.equals(NoOpTransformer.class.getName())) {
            return oldVal;
        }

        Function<Object, Object> transformer = constructTransformerInstance(attributeName);
        return transformer.apply(oldVal);
    }

    public void add(String delegateTarget, Replaces delegateAnnotation, Object value) {
        this.attributeReplacements.put(delegateTarget, new Replacement(delegateAnnotation, value));
    }

    public record Replacement(Replaces annotation, Object replacementValue) {
    }
}
