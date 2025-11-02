package dev.jackraidenph.libraomni.exception;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class AnnotationValidationException extends IllegalStateException {
    public AnnotationValidationException(String details, Throwable cause) {
        super(details, cause);
    }

    public AnnotationValidationException(String details) {
        this(details, null);
    }

    public AnnotationValidationException(Element e, TypeElement annotation, Throwable cause) {
        super("Failed to validate element [%s] for annotation [%s]".formatted(e, annotation.getSimpleName()), cause);
    }

    public AnnotationValidationException(Element e, TypeElement annotation) {
        this(e, annotation, null);
    }
}
