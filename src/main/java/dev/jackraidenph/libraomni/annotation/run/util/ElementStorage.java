package dev.jackraidenph.libraomni.annotation.run.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ElementStorage {

    private final Set<AnnotatedElement> annotatedElements = new HashSet<>();

    public void set(Set<AnnotatedElement> elements) {
        this.annotatedElements.clear();
        this.annotatedElements.addAll(elements);
    }

    public Set<Class<? extends Annotation>> getAnnotations() {
        return this.annotatedElements.stream()
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Arrays::stream)
                .map(Annotation::getClass)
                .collect(Collectors.toSet());
    }

    public Set<AnnotatedElement> elementsAnnotatedWith(Class<? extends Annotation> annotation) {
        return this.annotatedElements.stream().
                filter(a -> a.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    public Set<AnnotatedElement> elements() {
        return Set.copyOf(this.annotatedElements);
    }

    @Override
    public String toString() {
        return this.annotatedElements.toString();
    }
}
