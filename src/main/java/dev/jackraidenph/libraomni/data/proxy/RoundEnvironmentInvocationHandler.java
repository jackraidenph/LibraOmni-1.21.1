package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner14;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class RoundEnvironmentInvocationHandler extends ObjectPreservingInvocationHandler<RoundEnvironment> {

    private final Set<Element> proxiedElements = new HashSet<>();
    private final Elements elementUtils;

    public RoundEnvironmentInvocationHandler(RoundEnvironment original, ProcessingEnvironment processingEnvironment) {
        super(original);
        this.elementUtils = processingEnvironment.getElementUtils();
        for (Element e : original.getRootElements()) {
            Element proxy = (Element) ProxyFactory.proxifyAnnotatedConstruct(e, elementUtils);
            proxiedElements.add(proxy);
        }
    }

    public Set<? extends Element> getRootElementsProxy() {
        return Collections.unmodifiableSet(proxiedElements);
    }

    public Set<? extends Element> getElementsAnnotatedWithProxy(TypeElement a) {
        Set<Element> elements = new LinkedHashSet<>();
        RecursiveAnnotationScanner scanner = new RecursiveAnnotationScanner();
        for (Element e : getRootElementsProxy()) {
            elements.addAll(scanner.scan(e, a));
        }
        return elements;
    }

    public Set<? extends Element> getElementsAnnotatedWithProxy(Class<? extends Annotation> a) {
        TypeElement element = elementUtils.getTypeElement(a.getName());
        if (element == null) {
            return Set.of();
        }

        return getElementsAnnotatedWithProxy(element);
    }

    public Set<? extends Element> getElementsAnnotatedWithAnyProxy(Set<Class<? extends Annotation>> annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (Class<? extends Annotation> a : annotations) {
            elements.addAll(getElementsAnnotatedWithProxy(a));
        }
        return elements;
    }

    public Set<? extends Element> getElementsAnnotatedWithAnyProxy(TypeElement... annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (TypeElement a : annotations) {
            elements.addAll(getElementsAnnotatedWithProxy(a));
        }
        return elements;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();

        if (name.equals("getRootElements")) {
            return getRootElementsProxy();
        } else if (args != null) {
            Object arg0 = args[0];
            if (name.equals("getElementsAnnotatedWith")) {
                if (arg0 instanceof Class<?> annotationClass) {
                    //noinspection unchecked
                    return getElementsAnnotatedWithProxy((Class<? extends Annotation>) annotationClass);
                } else if (arg0 instanceof TypeElement annotationType) {
                    return getElementsAnnotatedWithProxy(annotationType);
                } else {
                    throw new UnsupportedOperationException();
                }
            } else if (name.equals("getElementsAnnotatedWithAny")) {
                if (arg0 instanceof Set<?> annotationSet) {
                    //noinspection unchecked
                    return getElementsAnnotatedWithAnyProxy((Set<Class<? extends Annotation>>) annotationSet);
                } else if (arg0.getClass().isArray() && TypeElement.class.isAssignableFrom(arg0.getClass().componentType())) {
                    return getElementsAnnotatedWithAnyProxy((TypeElement[]) arg0);
                }
            }
        }

        return super.invoke(proxy, method, args);
    }

    private Element tryProxify(Element e) {
        return e instanceof Proxy ? e : (Element) ProxyFactory.proxifyAnnotatedConstruct(e, elementUtils);
    }

    private class RecursiveAnnotationScanner extends ElementScanner14<Set<Element>, TypeElement> {

        private final Set<Element> elements = new HashSet<>();

        @Override
        public Set<Element> scan(Element e, TypeElement annotation) {
            checkViaTypeElement(e, annotation);
            e.accept(this, annotation);
            return elements;
        }

        public void checkViaTypeElement(Element e, TypeElement a) {
            Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(a.getQualifiedName().toString(), Annotation.class);
            if (clazz == null) {
                return;
            }

            checkViaClass(e, clazz);
        }

        //This might be EXTREMELY slow. Needs checking.
        private void checkViaClass(Element e, Class<? extends Annotation> a) {
            Element toCheck = tryProxify(e);
            if (toCheck.getAnnotation(a) != null) {
                elements.add(toCheck);
            }
        }
    }
}
