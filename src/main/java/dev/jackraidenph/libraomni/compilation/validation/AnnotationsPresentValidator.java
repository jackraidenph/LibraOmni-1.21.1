package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnotationsPresentValidator extends TypesValidator {
    @Override
    public void test(Element validatedElement, TypeElement validatedAnnotation, List<String> args, ProcessingContext processingContext) {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Arguments for [%s] must annotations at positions 0..".formatted(this.getClass().getSimpleName()));
        }
        Set<String> annotationsToBePresent = new HashSet<>(args);
        Set<String> actuallyPresent = new HashSet<>(args.size());

        Elements elements = processingContext.processingEnvironment().getElementUtils();
        for (AnnotationMirror mirror : validatedElement.getAnnotationMirrors()) {
            TypeElement typeElement = AnnotationMirrorUtil.toTypeElement(mirror);
            String mirrorName = elements.getBinaryName(typeElement).toString();
            actuallyPresent.add(mirrorName);
        }

        if (!annotationsToBePresent.containsAll(actuallyPresent)) {
            throw new AnnotationValidationException("Annotations of type %s must be present on [%s]".formatted(args, validatedElement));
        }
    }
}
