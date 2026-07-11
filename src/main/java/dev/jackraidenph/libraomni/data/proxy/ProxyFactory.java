package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.data.ModMetadataReader;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class ProxyFactory {

    private static final ClassLoader CLASSLOADER = ProxyFactory.class.getClassLoader();

    //A special-case set of meta-annotations that must not be considered transitively
    public static final Set<Class<? extends Annotation>> ONLY_DIRECT = Set.of(
            Target.class,
            Retention.class,
            Inherited.class,
            Repeatable.class,
            Documented.class,
            Composed.class,
            NeedsRuntimeProcessing.class,
            Validated.class
    );

    public static Annotation makeAnnotationProxy(Annotation annotation, AttributeReplacements delegates, Object annotatedElement, @Nullable ModIdGetter modIdGetter, @Nullable ModMetadataReader modMetadataReader) {
        if (annotation instanceof Composed || annotation.annotationType().getPackageName().startsWith("java.lang.annotation")) {
            return annotation;
        }
        if (delegates == null || delegates.isEmpty()) {
            return annotation;
        }
        return (Annotation) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{annotation.annotationType()},
                new AnnotationProxy(annotation, delegates, annotatedElement, modIdGetter, modMetadataReader)
        );
    }

    public static Annotation makeAnnotationProxy(Annotation annotation, AttributeReplacements delegates, Object annotatedElement, @Nullable ModIdGetter modIdGetter) {
        return makeAnnotationProxy(annotation, delegates, annotatedElement, modIdGetter, null);
    }

    public static Annotation makeAnnotationProxy(Annotation annotation, AttributeReplacements delegates, Object annotatedElement, @Nullable ModMetadataReader modMetadataReader) {
        return makeAnnotationProxy(annotation, delegates, annotatedElement, null, modMetadataReader);
    }

    private static AttributeReplacements mapDelegatesFromAnnotation(Class<? extends Annotation> childType, Annotation parent) {
        AttributeReplacements container = new AttributeReplacements(parent.annotationType().getName());
        for (Method attribute : parent.annotationType().getDeclaredMethods()) {
            Replaces delegate = attribute.getAnnotation(Replaces.class);
            if (delegate == null || !delegate.in().equals(childType)) {
                continue;
            }
            Object val = UnsafeReflectionUtil.getMethodValue(attribute, parent);
            container.add(delegate.attribute(), delegate, val);
        }
        return container;
    }

    public static Annotation makeAnnotationProxy(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModMetadataReader modMetadataReader) {
        return makeAnnotationProxy(child, parent, annotatedElement, null, modMetadataReader);
    }

    public static Annotation makeAnnotationProxy(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModIdGetter modIdGetter) {
        return makeAnnotationProxy(child, parent, annotatedElement, modIdGetter, null);
    }

    public static Annotation makeAnnotationProxy(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModIdGetter modIdGetter, @Nullable ModMetadataReader modMetadataReader) {
        if (parent == null) {
            return child;
        }
        AttributeReplacements delegates = mapDelegatesFromAnnotation(child.annotationType(), parent);
        return makeAnnotationProxy(child, delegates, annotatedElement, modIdGetter, modMetadataReader);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static TypeMirror getAnnotationTypeMirror(Replaces compileTimeDelegate) {
        try {
            compileTimeDelegate.in();
        } catch (MirroredTypeException mirroredTypeException) {
            return mirroredTypeException.getTypeMirror();
        }
        throw new IllegalStateException();
    }

    public static AttributeReplacements mapDelegatesFromAnnotationMirror(Elements elements, String childTypeName, AnnotationMirror parent, AttributeReplacements contextDelegates) {
        AttributeReplacements container = new AttributeReplacements(
                elements.getBinaryName(AnnotationMirrorUtil.toTypeElement(parent)).toString()
        );

        Map<ExecutableElement, AnnotationValue> values = new HashMap<>(elements.getElementValuesWithDefaults(parent));
        parent.getAnnotationType()
                .asElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(ExecutableElement::isDefault)
                .forEach(e -> values.put(e, e.getDefaultValue()));

        for (Entry<ExecutableElement, AnnotationValue> attributeValue : values.entrySet()) {
            ExecutableElement executableElement = attributeValue.getKey();
            AnnotationValue annotationValue = attributeValue.getValue();
            Replaces delegate = executableElement.getAnnotation(Replaces.class);
            if (delegate == null) {
                continue;
            }

            if (!getAnnotationTypeMirror(delegate).toString().equals(childTypeName)) {
                continue;
            }

            String name = executableElement.getSimpleName().toString();
            Object attributeVal = contextDelegates != null && contextDelegates.hasReplacementFor(name)
                    ? contextDelegates.getReplacementValue(name)
                    : annotationValue.getValue();
            container.add(delegate.attribute(), delegate, attributeVal);
        }
        return container;
    }

    public static ProxyAnnotatedElement makeAnnotatedElementProxy(AnnotatedElement element, ModMetadataReader modMetadataReader) {
        return (ProxyAnnotatedElement) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{ProxyAnnotatedElement.class},
                new AnnotatedElementProxy(element, modMetadataReader)
        );
    }

    public static AnnotatedConstruct makeAnnotatedConstructProxy(AnnotatedConstruct construct, Elements elements, ModIdGetter modIdGetter) {
        return (AnnotatedConstruct) Proxy.newProxyInstance(
                CLASSLOADER,
                construct.getClass().getInterfaces(),
                new AnnotatedConstructProxy(construct, elements, modIdGetter)
        );
    }

    public static AnnotatedConstruct tryMakeAnnotatedConstructProxy(AnnotatedConstruct e, Elements elements, ModIdGetter modIdGetter) {
        return e instanceof Proxy ? e : (Element) ProxyFactory.makeAnnotatedConstructProxy(e, elements, modIdGetter);
    }


    public static ProxyAnnotatedElement tryMakeAnnotatedElementProxy(AnnotatedElement element, ModMetadataReader modMetadataReader) {
        //Proxy handler implements AnnotationAccessor
        return (ProxyAnnotatedElement) (element instanceof Proxy ? element : ProxyFactory.makeAnnotatedElementProxy(element, modMetadataReader));
    }

    protected static <T extends Annotation> T makeValueAnnotation(Class<T> type, Map<String, Object> attributes) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{type},
                new SyntheticAnnotation<>(type, attributes)
        );
    }

    public static RoundEnvironment makeRuntimeEnvironmentProxy(RoundEnvironment environment, ProcessingEnvironment processingEnvironment, ModIdGetter modIdGetter) {
        return (RoundEnvironment) Proxy.newProxyInstance(
                CLASSLOADER,
                environment.getClass().getInterfaces(),
                new RoundEnvironmentProxy(environment, processingEnvironment, modIdGetter)
        );
    }
}
