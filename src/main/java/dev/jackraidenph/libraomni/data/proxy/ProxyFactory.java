package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;
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

    public static Annotation proxifyAnnotation(Annotation annotation, DelegateContainer delegates, Object annotatedElement, @Nullable ModIdGetter modIdGetter, @Nullable ModMetadataReader modMetadataReader) {
        if (annotation instanceof Composed || annotation.annotationType().getPackageName().startsWith("java.lang.annotation")) {
            return annotation;
        }
        if (delegates == null || delegates.isEmpty()) {
            return annotation;
        }
        return (Annotation) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{annotation.annotationType()},
                new DelegatingAnnotationInvocationHandler(annotation, delegates, annotatedElement, modIdGetter, modMetadataReader)
        );
    }

    public static Annotation proxifyAnnotation(Annotation annotation, DelegateContainer delegates, Object annotatedElement, @Nullable ModIdGetter modIdGetter) {
        return proxifyAnnotation(annotation, delegates, annotatedElement, modIdGetter, null);
    }

    public static Annotation proxifyAnnotation(Annotation annotation, DelegateContainer delegates, Object annotatedElement, @Nullable ModMetadataReader modMetadataReader) {
        return proxifyAnnotation(annotation, delegates, annotatedElement, null, modMetadataReader);
    }

    private static DelegateContainer mapDelegatesFromAnnotation(Class<? extends Annotation> childType, Annotation parent) {
        DelegateContainer container = new DelegateContainer(parent.annotationType().getName());
        for (Method attribute : parent.annotationType().getDeclaredMethods()) {
            Delegate delegate = attribute.getAnnotation(Delegate.class);
            if (delegate == null || !delegate.annotation().equals(childType)) {
                continue;
            }
            Object val = UnsafeReflectionUtil.getMethodValue(attribute, parent);
            container.add(delegate.attribute(), delegate, val);
        }
        return container;
    }

    public static Annotation proxifyAnnotation(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModMetadataReader modMetadataReader) {
        return proxifyAnnotation(child, parent, annotatedElement, null, modMetadataReader);
    }

    public static Annotation proxifyAnnotation(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModIdGetter modIdGetter) {
        return proxifyAnnotation(child, parent, annotatedElement, modIdGetter, null);
    }

    public static Annotation proxifyAnnotation(Annotation child, Annotation parent, Object annotatedElement, @Nullable ModIdGetter modIdGetter, @Nullable ModMetadataReader modMetadataReader) {
        if (parent == null) {
            return child;
        }
        DelegateContainer delegates = mapDelegatesFromAnnotation(child.annotationType(), parent);
        return proxifyAnnotation(child, delegates, annotatedElement, modIdGetter, modMetadataReader);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static TypeMirror getAnnotation(Delegate compileTimeDelegate) {
        try {
            compileTimeDelegate.annotation();
        } catch (MirroredTypeException mirroredTypeException) {
            return mirroredTypeException.getTypeMirror();
        }
        throw new IllegalStateException();
    }

    public static DelegateContainer mapDelegatesFromAnnotationMirror(Elements elements, String childTypeName, AnnotationMirror parent, DelegateContainer contextDelegates) {
        DelegateContainer container = new DelegateContainer(
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
            Delegate delegate = executableElement.getAnnotation(Delegate.class);
            if (delegate == null) {
                continue;
            }

            if (!getAnnotation(delegate).toString().equals(childTypeName)) {
                continue;
            }

            String name = executableElement.getSimpleName().toString();
            Object attributeVal = contextDelegates != null && contextDelegates.hasDelegateFor(name)
                    ? contextDelegates.getDelegatedValue(name)
                    : annotationValue.getValue();
            container.add(delegate.attribute(), delegate, attributeVal);
        }
        return container;
    }

    public static ProxyAnnotatedElement proxifyAnnotatedElement(AnnotatedElement element, ModMetadataReader modMetadataReader) {
        return (ProxyAnnotatedElement) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{ProxyAnnotatedElement.class},
                new AnnotatedElementInvocationHandler(element, modMetadataReader)
        );
    }

    public static AnnotatedConstruct proxifyAnnotatedConstruct(AnnotatedConstruct construct, Elements elements, ModIdGetter modIdGetter) {
        return (AnnotatedConstruct) Proxy.newProxyInstance(
                CLASSLOADER,
                construct.getClass().getInterfaces(),
                new AnnotatedConstructInvocationHandler(construct, elements, modIdGetter)
        );
    }

    public static AnnotatedConstruct proxifyAnnotatedConstructIfNotProxy(AnnotatedConstruct e, Elements elements, ModIdGetter modIdGetter) {
        return e instanceof Proxy ? e : (Element) ProxyFactory.proxifyAnnotatedConstruct(e, elements, modIdGetter);
    }


    public static ProxyAnnotatedElement proxifyAnnotatedElementIfNotProxy(AnnotatedElement element, ModMetadataReader modMetadataReader) {
        //Proxy handler implements AnnotationAccessor
        return (ProxyAnnotatedElement) (element instanceof Proxy ? element : ProxyFactory.proxifyAnnotatedElement(element, modMetadataReader));
    }

    public static <T extends Annotation> T makeValueAnnotation(Class<T> type, Map<String, Object> attributes) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{type},
                new ValueAnnotationInvocationHandler(type, attributes)
        );
    }

    public static RoundEnvironment proxifyRuntimeEnvironment(RoundEnvironment environment, ProcessingEnvironment processingEnvironment, ModIdGetter modIdGetter) {
        return (RoundEnvironment) Proxy.newProxyInstance(
                CLASSLOADER,
                environment.getClass().getInterfaces(),
                new RoundEnvironmentInvocationHandler(environment, processingEnvironment, modIdGetter)
        );
    }
}
