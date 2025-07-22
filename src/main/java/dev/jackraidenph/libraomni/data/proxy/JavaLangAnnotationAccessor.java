package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaLangAnnotationAccessor implements AnnotationAccessor<AnnotatedConstruct> {

    private final AnnotatedConstruct annotatedConstruct;

    public JavaLangAnnotationAccessor(AnnotatedConstruct construct) {
        this.annotatedConstruct = construct;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        try {
            List<Annotation> annotations = new ArrayList<>();
            for (AnnotationMirror mirror : this.annotatedConstruct.getAnnotationMirrors()) {
                TypeElement typeElement = (TypeElement) mirror.getAnnotationType().asElement();
                Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(typeElement.getQualifiedName().toString(), Annotation.class);
                Annotation annotation = annotatedConstruct.getAnnotation(clazz);
                annotations.add(annotation);
            }
            return annotations;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AnnotatedConstruct unwrap() {
        return this.annotatedConstruct;
    }
}
