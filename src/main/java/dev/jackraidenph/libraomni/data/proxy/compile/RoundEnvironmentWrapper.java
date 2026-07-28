package dev.jackraidenph.libraomni.data.proxy.compile;

import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ElementUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class RoundEnvironmentWrapper implements RoundEnvironment {

    private final Set<Element> proxiedRootElements = new LinkedHashSet<>();
    private final SequencedSet<Element> allProxiedElements = new LinkedHashSet<>();
    private final Map<String, Set<Element>> elementsByAnnotationName = new LinkedHashMap<>();

    private final Elements elementUtils;
    private final RoundEnvironment wrapped;

    public RoundEnvironmentWrapper(RoundEnvironment original, ProcessingEnvironment processingEnvironment) {
        this.wrapped = original;
        this.elementUtils = processingEnvironment.getElementUtils();
        cacheElements();
    }

    private void cacheElements() {
        for (Element e : wrapped.getRootElements()) {
            proxiedRootElements.add((Element) ProxyFactory.makeAnnotatedConstructProxy(e));
            addElementAndRecurse(e);
        }
    }

    private void addElementAndRecurse(Element e) {
        Element proxy = (Element) ProxyFactory.makeAnnotatedConstructProxy(e);

        allProxiedElements.add(proxy);

        for (AnnotationMirror mirror : ElementUtil.Javac.getAllAnnotationMirrors(proxy)) {
            TypeElement typeElement = AnnotationMirrorUtil.toTypeElement(mirror);
            String annotationName = typeElement.getQualifiedName().toString();
            elementsByAnnotationName.computeIfAbsent(annotationName, k -> new LinkedHashSet<>()).add(proxy);
        }

        for (Element enclosed : proxy.getEnclosedElements()) {
            addElementAndRecurse(enclosed);
        }
    }

    public Set<Element> getAllElements() {
        return Collections.unmodifiableSequencedSet(allProxiedElements);
    }

    public Set<TypeElement> getAllAnnotationTypes() {
        return elementsByAnnotationName.keySet().stream()
                .map(elementUtils::getTypeElement)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
        return elementsByAnnotationName.getOrDefault(annotationName, Set.of());
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
