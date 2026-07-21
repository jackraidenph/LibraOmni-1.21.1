package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.data.proxy.compile.AnnotatedConstructProxy;
import dev.jackraidenph.libraomni.data.proxy.compile.RoundEnvironmentWrapper;
import dev.jackraidenph.libraomni.data.proxy.runtime.AnnotatedElementWrapper;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;

public abstract class ProxyFactory {

    private static final ClassLoader CLASSLOADER = ProxyFactory.class.getClassLoader();

    public static ProxiedAnnotatedElement makeAnnotatedElementProxy(AnnotatedElement element) {
        if (element instanceof AnnotatedElementWrapper) {
            return (ProxiedAnnotatedElement) element;
        }

        return new AnnotatedElementWrapper(element);
    }

    public static AnnotatedConstruct makeAnnotatedConstructProxy(AnnotatedConstruct construct) {
        if (Proxy.isProxyClass(construct.getClass())) {
            return construct;
        }

        return (AnnotatedConstruct) Proxy.newProxyInstance(
                CLASSLOADER,
                construct.getClass().getInterfaces(),
                new AnnotatedConstructProxy(construct)
        );
    }

    public static <T extends Annotation> T sythesizeAnnotation(Class<T> type, Map<String, Object> attributes) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{type},
                new SyntheticAnnotation<>(type, attributes)
        );
    }

    public static RoundEnvironment makeRuntimeEnvironmentProxy(RoundEnvironment environment, ProcessingEnvironment processingEnvironment) {
        return new RoundEnvironmentWrapper(environment, processingEnvironment);
    }
}
