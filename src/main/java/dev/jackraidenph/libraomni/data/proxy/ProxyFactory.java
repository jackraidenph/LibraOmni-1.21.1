package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.annotation.Delegate;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ProxyFactory {

    private static final ClassLoader CLASSLOADER = ProxyFactory.class.getClassLoader();

    public static Annotation proxifyAnnotation(Annotation annotation, DelegateContainer delegates) {
        if (annotation instanceof Composed || annotation.annotationType().getPackageName().startsWith("java.lang.annotation")) {
            return annotation;
        }
        if (delegates == null || delegates.isEmpty()) {
            return annotation;
        }
        return (Annotation) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{annotation.annotationType()},
                new DelegatingAnnotationInvocationHandler(annotation, delegates)
        );
    }

    private static DelegateContainer mapDelegatesFromAnnotation(Class<? extends Annotation> childType, Annotation parent) {
        DelegateContainer container = new DelegateContainer();
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

    public static Annotation proxifyAnnotation(Annotation child, Annotation parent) {
        if (parent == null) {
            return child;
        }
        DelegateContainer delegates = mapDelegatesFromAnnotation(child.annotationType(), parent);
        return proxifyAnnotation(child, delegates);
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

    public static DelegateContainer mapDelegatesFromAnnotationMirror(String childTypeName, AnnotationMirror parent) {
        return mapDelegatesFromAnnotationMirror(childTypeName, parent, null);
    }

    public static DelegateContainer mapDelegatesFromAnnotationMirror(String childTypeName, AnnotationMirror parent, DelegateContainer contextDelegates) {
        DelegateContainer container = new DelegateContainer();

        Map<ExecutableElement, AnnotationValue> values = new HashMap<>(parent.getElementValues());
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

    public static Annotation proxifyAnnotation(Annotation child, AnnotationMirror parent) {
        if (parent == null) {
            return child;
        }
        DelegateContainer delegates = mapDelegatesFromAnnotationMirror(child.annotationType().getName(), parent);
        return proxifyAnnotation(child, delegates);
    }

    public static AnnotatedElement proxifyAnnotatedElement(AnnotatedElement element) {
        return (AnnotatedElement) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{AnnotatedElement.class, AnnotationAccessor.class},
                new AnnotationCachingInvocationHandler(element)
        );
    }

    public static AnnotatedConstruct proxifyAnnotatedConstruct(AnnotatedConstruct construct) {
        return (AnnotatedConstruct) Proxy.newProxyInstance(
                CLASSLOADER,
                construct.getClass().getInterfaces(),
                new AnnotationMirrorCachingInvocationHandler(construct)
        );
    }

    public static <T extends Annotation> T makeValueAnnotation(Class<T> type, Map<String, Object> attributes) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{type},
                new AnnotationInvocationHandler(type, attributes)
        );
    }

    public static RoundEnvironment proxifyRuntimeEnvironment(RoundEnvironment environment, ProcessingEnvironment processingEnvironment) {
        return (RoundEnvironment) Proxy.newProxyInstance(
                CLASSLOADER,
                environment.getClass().getInterfaces(),
                new RoundEnvironmentInvocationHandler(environment, processingEnvironment)
        );
    }
}
