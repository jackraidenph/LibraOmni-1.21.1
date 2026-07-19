package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
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
            Element proxy = (Element) ProxyFactory.makeAnnotatedConstructProxy(e, modIdGetter);
            proxiedRootElements.add(proxy);
        }
    }

    @InterceptorFor("getRootElements")
    public Set<? extends Element> getRootElements() {
        return Collections.unmodifiableSet(proxiedRootElements);
    }

    @InterceptorFor("getElementsAnnotatedWith")
    public Set<? extends Element> getElementsAnnotatedWith(TypeElement annotationTypeElement) {
        String annotationName = annotationTypeElement.getQualifiedName().toString();

        if (!scannedElementsCacheByAnnotation.containsKey(annotationName)) {
            RecursiveAnnotationScanner scanner = new RecursiveAnnotationScanner(modIdGetter);
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

    @InterceptorFor("getElementsAnnotatedWith")
    public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        TypeElement element = elementUtils.getTypeElement(annotationClass.getName());
        if (element == null) {
            return Set.of();
        }

        return getElementsAnnotatedWith(element);
    }

    @InterceptorFor("getElementsAnnotatedWithAny")
    public Set<? extends Element> getElementsAnnotatedWithAny(Set<Class<? extends Annotation>> annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (Class<? extends Annotation> a : annotations) {
            elements.addAll(getElementsAnnotatedWith(a));
        }
        return elements;
    }

    @InterceptorFor("getElementsAnnotatedWithAny")
    public Set<? extends Element> getElementsAnnotatedWithAny(TypeElement... annotations) {
        Set<Element> elements = new LinkedHashSet<>();
        for (TypeElement a : annotations) {
            elements.addAll(getElementsAnnotatedWith(a));
        }
        return elements;
    }
}
