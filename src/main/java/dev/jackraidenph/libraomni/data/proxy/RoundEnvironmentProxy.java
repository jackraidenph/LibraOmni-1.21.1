package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class RoundEnvironmentProxy extends AbstractObjectProxy<RoundEnvironment> {

    private final Set<Element> proxiedRootElements = new HashSet<>();
    private final Elements elementUtils;
    private final ModIdGetter modIdGetter;
    private final Map<String, Set<? extends Element>> scannedElementsCacheByAnnotation = new HashMap<>();

    public RoundEnvironmentProxy(RoundEnvironment original, ProcessingEnvironment processingEnvironment, ModIdGetter modIdGetter) {
        super(original);
        this.elementUtils = processingEnvironment.getElementUtils();
        this.modIdGetter = modIdGetter;
        for (Element e : original.getRootElements()) {
            Element proxy = (Element) ProxyFactory.tryMakeAnnotatedConstructProxy(e, elementUtils, modIdGetter);
            proxiedRootElements.add(proxy);
        }
    }

    public Set<? extends Element> getRootElementsProxy() {
        return Collections.unmodifiableSet(proxiedRootElements);
    }

    public Set<? extends Element> getElementsAnnotatedWithProxy(TypeElement annotationTypeElement) {
        String annotationName = annotationTypeElement.getQualifiedName().toString();
        if (!scannedElementsCacheByAnnotation.containsKey(annotationName)) {
            RecursiveAnnotationScanner scanner = new RecursiveAnnotationScanner(elementUtils, modIdGetter);
            for (Element e : getRootElementsProxy()) {
                scanner.scan(e, annotationTypeElement);
            }
            scannedElementsCacheByAnnotation.put(annotationName, scanner.getElements());
        }
        Set<? extends Element> elements = scannedElementsCacheByAnnotation.get(annotationName);
        if (elements == null) {
            throw new IllegalStateException("Elements cache is null");
        }
        return elements;
    }

    public Set<? extends Element> getElementsAnnotatedWithProxy(Class<? extends Annotation> annotationClass) {
        TypeElement element = elementUtils.getTypeElement(annotationClass.getName());
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
}
