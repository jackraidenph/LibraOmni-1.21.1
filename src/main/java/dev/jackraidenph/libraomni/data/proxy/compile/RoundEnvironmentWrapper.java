package dev.jackraidenph.libraomni.data.proxy.compile;

import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;

public class RoundEnvironmentWrapper implements RoundEnvironment {

    private final Set<Element> proxiedRootElements = new HashSet<>();
    private final Elements elementUtils;
    private final Map<String, Set<? extends Element>> scannedElementsCacheByAnnotation = new HashMap<>();
    private final RoundEnvironment wrapped;

    public RoundEnvironmentWrapper(RoundEnvironment original, ProcessingEnvironment processingEnvironment) {
        this.wrapped = original;
        this.elementUtils = processingEnvironment.getElementUtils();
        for (Element e : original.getRootElements()) {
            Element proxy = (Element) ProxyFactory.makeAnnotatedConstructProxy(e);
            proxiedRootElements.add(proxy);
        }
    }

    @Override
    public boolean processingOver() {
        return wrapped.processingOver();
    }

    @Override
    public boolean errorRaised() {
        return wrapped.errorRaised();
    }

    @Override
    public Set<? extends Element> getRootElements() {
        return Collections.unmodifiableSet(proxiedRootElements);
    }

    @Override
    public Set<? extends Element> getElementsAnnotatedWith(TypeElement annotationTypeElement) {
        String annotationName = annotationTypeElement.getQualifiedName().toString();

        if (!scannedElementsCacheByAnnotation.containsKey(annotationName)) {
            RecursiveAnnotationScanner scanner = new RecursiveAnnotationScanner();
            for (Element e : getRootElements()) {
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

    @Override
    public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        TypeElement element = elementUtils.getTypeElement(annotationClass.getName());
        if (element == null) {
            return Set.of();
        }

        return getElementsAnnotatedWith(element);
    }

    @Override
    public Set<? extends Element> getElementsAnnotatedWithAny(Set<Class<? extends Annotation>> annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (Class<? extends Annotation> a : annotations) {
            elements.addAll(getElementsAnnotatedWith(a));
        }
        return elements;
    }

    @Override
    public Set<? extends Element> getElementsAnnotatedWithAny(TypeElement... annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (TypeElement a : annotations) {
            elements.addAll(getElementsAnnotatedWith(a));
        }
        return elements;
    }

    @Override
    public String toString() {
        return "Wrapped@" + wrapped;
    }
}
