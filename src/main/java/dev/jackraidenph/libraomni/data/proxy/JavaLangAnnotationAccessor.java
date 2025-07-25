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

    /**
     * @return In this particular case, ONLY annotations compiled and loaded during annotation processor run are returned
     */
    @Override
    public Collection<Annotation> getAllAnnotations() {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationMirror mirror : this.annotatedConstruct.getAnnotationMirrors()) {
            TypeElement typeElement = (TypeElement) mirror.getAnnotationType().asElement();
            String name = typeElement.getQualifiedName().toString();
            Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(name, Annotation.class);
            if (clazz == null) {
                continue;
            }
            Annotation annotation = annotatedConstruct.getAnnotation(clazz);
            annotations.add(annotation);
        }
        return annotations;
    }

    @Override
    public AnnotatedConstruct annotatedObject() {
        return this.annotatedConstruct;
    }
}
