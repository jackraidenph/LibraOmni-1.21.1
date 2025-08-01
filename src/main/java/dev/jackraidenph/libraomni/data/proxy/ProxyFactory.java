package dev.jackraidenph.libraomni.data.proxy;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.Map;

public abstract class ProxyFactory {

    private static final ClassLoader CLASSLOADER = ProxyFactory.class.getClassLoader();

    public static <T extends Annotation, R extends Annotation> T proxifyAnnotation(T child, R parent) {
        if (parent == null) {
            return child;
        }
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{child.annotationType()},
                new DelegatingAnnotationInvocationHandler(child, parent)
        );
    }

    public static AnnotatedElement proxifyAnnotatedElement(AnnotatedElement element) {
        return (AnnotatedElement) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{AnnotatedElement.class, AnnotationAccessor.class},
                new AnnotatedElementInvocationHandler(element)
        );
    }

    public static AnnotatedConstruct proxifyAnnotatedConstruct(AnnotatedConstruct construct) {
        return (AnnotatedConstruct) Proxy.newProxyInstance(
                CLASSLOADER,
                construct.getClass().getInterfaces(),
                new AnnotatedConstructInvocationHandler(construct)
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
