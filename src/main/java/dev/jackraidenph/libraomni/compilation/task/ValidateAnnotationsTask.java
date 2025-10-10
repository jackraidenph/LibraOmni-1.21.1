package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.service.Validated;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.validation.Validator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.Set;
import java.util.stream.Collectors;

final class ValidateAnnotationsTask implements CompilationTask {

    @Override
    public Set<InMemoryResource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Set<TypeElement> validatedAnnotations = roundEnv
                .getRootElements()
                .stream()
                .flatMap(e -> e.getAnnotationMirrors().stream())
                .map(am -> (TypeElement) am.getAnnotationType().asElement())
                .filter(e -> e.getAnnotation(Validated.class) != null)
                .collect(Collectors.toSet());

        for (TypeElement annotationElement : validatedAnnotations) {
            Validator validator = this.getValidatorForAnnotation(annotationElement);
            if (validator == null) {
                throw new AnnotationValidationException(annotationElement, "Failed to instantiate validator");
            }
            processingEnv.getMessager().printNote("Found validator [" + validator.getClass().getSimpleName() + "] for [" + annotationElement.getQualifiedName() + "]");

            Set<? extends Element> toValidate = roundEnv.getElementsAnnotatedWith(annotationElement);

            for (Element e : toValidate) {
                try {
                    if (!e.getKind().equals(ElementKind.ANNOTATION_TYPE) && !validator.test(e, processingEnv)) {
                        throw new AnnotationValidationException(e);
                    }
                } catch (Exception innerException) {
                    throw new AnnotationValidationException(e, innerException);
                }
            }
        }

        return Set.of();
    }

    private AnnotationMirror mirrorByClass(Element e, Class<?> clazz) {
        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().contentEquals(clazz.getName())) {
                return annotationMirror;
            }
        }

        return null;
    }

    private static Object findAnnotationValue(AnnotationMirror mirror, String valueName) {
        ExecutableElement executableElement = mirror.getElementValues()
                .keySet()
                .stream()
                .filter(e -> e.getSimpleName().contentEquals(valueName))
                .findFirst()
                .orElse(null);

        return mirror.getElementValues().get(executableElement).getValue();
    }

    private Validator getValidatorForAnnotation(TypeElement annotationElement) {
        AnnotationMirror validatedMirror = mirrorByClass(annotationElement, Validated.class);
        if (validatedMirror == null) {
            return null;
        }

        try {
            String validatorClassName = String.valueOf(findAnnotationValue(validatedMirror, "value"));
            Class<? extends Validator> validatorClass = SafeReflectionUtil.forNameSubclass(validatorClassName, Validator.class);
            if (validatorClass == null) {
                throw new IllegalStateException("Failed to get Validator for name [" + validatorClassName + "]");
            }

            return UnsafeReflectionUtil.tryConstruct(validatorClass);
        } catch (ClassCastException classCastException) {
            return null;
        }
    }

    public static class AnnotationValidationException extends IllegalStateException {
        public AnnotationValidationException(Element e, String details) {
            super("Validation failed for [%s]: %s".formatted(e, details));
        }

        public AnnotationValidationException(Element e, Throwable cause) {
            super("Validation failed for [%s]".formatted(e), cause);
        }


        public AnnotationValidationException(Element e) {
            super("Validation failed for [%s]".formatted(e));
        }
    }
}
