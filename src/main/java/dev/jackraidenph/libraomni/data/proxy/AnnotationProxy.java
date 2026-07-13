package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.data.ModMetadataReader;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationProxy extends AbstractObjectProxy<Annotation> {

    private final AttributeReplacements attributeReplacements;
    private final Object annotated;
    private final ModIdGetter modIdGetter;
    private final ModMetadataReader modMetadataReader;

    public AnnotationProxy(Annotation proxiedObject, AttributeReplacements attributeReplacements, Object annotated, @Nullable ModIdGetter modIdGetter, @Nullable ModMetadataReader modMetadataReader) {
        super(proxiedObject);
        Set<String> nonExistent = attributeReplacements.getNonCommonMethods(proxiedObject);
        if (!nonExistent.isEmpty()) {
            throw new IllegalStateException("Can't delegate methods %s from [%s] that don't exist in [%s]"
                    .formatted(nonExistent, attributeReplacements.getParentAnotationBinaryName(), proxiedObject));
        }
        this.attributeReplacements = attributeReplacements;
        this.annotated = annotated;
        this.modIdGetter = modIdGetter;
        this.modMetadataReader = modMetadataReader;
    }

    private boolean checkReturnType(Class<?> oldReturnType, Class<?> newReturnType) {
        return oldReturnType.isAssignableFrom(newReturnType)
                | (oldReturnType.isArray() && oldReturnType.componentType().isAssignableFrom(newReturnType));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();

        if (name.equals("toString")) {
            return toStringProxy((Annotation) proxy);
        }

        if (!attributeReplacements.hasReplacementFor(name)) {
            Object value = super.invoke(proxy, method, args);
            return tryReplacePlaceholdersIfString(value);
        }

        Object replacementValue = attributeReplacements.getReplacementValue(name);
        Object transformedReplacementValue = attributeReplacements.getTransformedReplacementValue(name);
        if (!replacementValue.equals(transformedReplacementValue)) {
            Class<?> oldReturnType = method.getReturnType();
            Class<?> newReturnType = (transformedReplacementValue instanceof Annotation annotation) ? annotation.annotationType() : transformedReplacementValue.getClass();
            if (!checkReturnType(oldReturnType, newReturnType)) {
                throw new IllegalStateException("Couldn't transform value [%s] of [%s], value of type [%s] is not applicable to [%s] "
                        .formatted(
                                name,
                                proxiedObject.annotationType(),
                                newReturnType.getName(),
                                oldReturnType.getName()
                        ));
            }
        }

        transformedReplacementValue = tryReplacePlaceholdersIfString(transformedReplacementValue);
        return SafeReflectionUtil.selfOrSingletonArray(method.getReturnType(), transformedReplacementValue);
    }

    private Object tryReplacePlaceholdersIfString(Object value) {
        if (!(value instanceof String str) || str.isBlank() || str.indexOf('{') < 0) {
            return value;
        }

        str = str.replace("{mod_id}", getModId());

        String id = switch (annotated) {
            case AnnotatedElement annotatedElement -> SafeReflectionUtil.idOrDefault(annotatedElement);
            case Element annotatedConstruct -> ModIdGetter.getElementId(annotatedConstruct);
            default ->
                    throw new IllegalStateException("Object is not either of AnnotatedElement or AnnotatedConstruct, this shouldn't be possible?");
        };

        str = str.replace("{element_id}", id);

        return str;
    }

    private String getModId() {
        if (modIdGetter != null) {
            return modIdGetter.forElement((Element) annotated);
        } else if (modMetadataReader != null) {
            return modMetadataReader.modIdOfElement((AnnotatedElement) annotated);
        }

        throw new IllegalStateException("Both ModIdGetter and ModMetadataReader are null");
    }

    private String toStringProxy(Annotation proxy) {
        StringBuilder builder = new StringBuilder();
        builder.append('@').append(proxy.annotationType().getName());
        String methods = Arrays.stream(proxy.annotationType().getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(m -> methodToString(proxy, m))
                .collect(Collectors.joining(",", "(", ")"));
        return builder.append(methods).toString();
    }

    private String methodToString(Annotation proxy, Method annotationMethod) {
        Object val = invoke(proxy, annotationMethod, new Object[0]);
        String str = val.getClass().isArray() ? arrayToString((Object[]) val) : objToStr(val);
        return annotationMethod.getName() + "=" + str;
    }

    private String arrayToString(Object[] arr) {
        return arr.length == 0 ? "{}" : Arrays.stream(arr).map(this::objToStr).collect(Collectors.joining(",", "{", "}"));
    }

    private String objToStr(Object o) {
        if (o instanceof String str) {
            return StringUtilities.quote(str);
        } else if (o instanceof Class<?> clazz) {
            return clazz.getName();
        }

        return String.valueOf(o);
    }
}
