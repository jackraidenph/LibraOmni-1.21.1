package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.data.proxy.compile.AnnotatedConstructProxy;
import dev.jackraidenph.libraomni.data.proxy.compile.RoundEnvironmentProxy;
import dev.jackraidenph.libraomni.data.proxy.runtime.AnnotatedElementProxy;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.data.proxy.runtime.SyntheticAnnotation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.Map;

public abstract class ProxyFactory {

    private static final ClassLoader CLASSLOADER = ProxyFactory.class.getClassLoader();

    public static ProxiedAnnotatedElement makeAnnotatedElementProxy(AnnotatedElement element) {
        if (element instanceof Proxy) {
            return (ProxiedAnnotatedElement) element;
        }

        return (ProxiedAnnotatedElement) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{ProxiedAnnotatedElement.class},
                new AnnotatedElementProxy(element)
        );
    }

    public static AnnotatedConstruct makeAnnotatedConstructProxy(AnnotatedConstruct construct, ModIdGetter modIdGetter) {
        if (construct instanceof Proxy) {
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

    public static RoundEnvironment makeRuntimeEnvironmentProxy(RoundEnvironment environment, ProcessingEnvironment processingEnvironment, ModIdGetter modIdGetter) {
        return (RoundEnvironment) Proxy.newProxyInstance(
                CLASSLOADER,
                new Class[]{RoundEnvironment.class},
                new RoundEnvironmentProxy(environment, processingEnvironment, modIdGetter)
        );
    }
}
