package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.validation.Validator;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ValidateAnnotationsTask implements CompilationTask {

    @Override
    public Set<InMemoryResource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Messager messager = processingEnv.getMessager();

        messager.printNote("---VALIDATING ANNOTATION---");

        Set<TypeElement> validatedAnnotations = getRequiringValidation(roundEnv);

        for (TypeElement validatedAnnotation : validatedAnnotations) {
            Validator validator = this.getValidatorForAnnotation(validatedAnnotation);
            List<String> args = getArgs(validatedAnnotation);
            if (args == null) {
                messager.printNote("Found validator [%s] for [@%s]".formatted(validator.getClass(), validatedAnnotation.getSimpleName()));
            } else {
                messager.printNote("Found validator [%s] with arguments %s for [@%s]".formatted(
                        validator.getClass(),
                        args,
                        validatedAnnotation.getSimpleName()
                ));
            }

            roundEnv.getElementsAnnotatedWith(validatedAnnotation).stream()
                    .filter(e -> !(e.getKind().equals(ElementKind.ANNOTATION_TYPE)))
                    .forEach(validatedElement -> {
                        validate(validatedElement, validatedAnnotation, args, validator, processingEnv);
                        messager.printNote("[%s] was validated with no problems".formatted(validatedElement));
                    });
        }

        messager.printNote("---ANNOTATIONS SUCCESSFULLY VALIDATED---");

        return Set.of();
    }

    private void validate(Element validatedElement,
                          TypeElement validatedAnnotation,
                          List<String> args,
                          Validator validator,
                          ProcessingEnvironment pEnv
    ) {
        if (validatedElement.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
            return;
        }

        boolean result;
        try {
            result = validator.test(validatedElement, args, pEnv);
        } catch (Exception innerException) {
            throw new AnnotationValidationException(validatedElement, validatedAnnotation, innerException);
        }

        if (!result) {
            throw new AnnotationValidationException(validatedElement, validatedAnnotation);
        }
    }

    private Set<TypeElement> getRequiringValidation(RoundEnvironment roundEnvironment) {
        return roundEnvironment
                .getRootElements()
                .stream()
                .map(Element::getAnnotationMirrors)
                .flatMap(List::stream)
                .map(AnnotationMirror::getAnnotationType)
                .map(declaredType -> (TypeElement) declaredType.asElement())
                .filter(e -> e.getAnnotation(Validated.class) != null)
                .collect(Collectors.toSet());
    }

    private List<String> getArgs(TypeElement validatedAnnotation) {
        AnnotationMirror validatorMirror = getValidatorAnnotationMirror(validatedAnnotation);

        Object objArgs = AnnotationMirrorUtil.getElementValue(validatorMirror, "args");
        if (objArgs instanceof List<?> list) {
            return list.stream()
                    .map(e -> (AnnotationValue) e)
                    .map(AnnotationValue::getValue)
                    .map(String::valueOf)
                    .toList();
        } else {
            return Collections.singletonList(String.valueOf(objArgs));
        }
    }

    private AnnotationMirror getValidatorAnnotationMirror(TypeElement validatedAnnotation) {
        AnnotationMirror validatedMirror = AnnotationMirrorUtil.findAnnotationMirror(
                validatedAnnotation::getAnnotationMirrors,
                Validated.class.getName()
        );

        if (validatedMirror == null) {
            throw new IllegalStateException("Couldn't obtain AnnotationMirror of [%s] for annotation name [%s]".formatted(
                    validatedAnnotation,
                    Validated.class.getName())
            );
        }

        return validatedMirror;
    }

    private Validator getValidatorForAnnotation(TypeElement validatedAnnotation) {
        AnnotationMirror validatorMirror = getValidatorAnnotationMirror(validatedAnnotation);
        String validatorClassName = String.valueOf(AnnotationMirrorUtil.getElementValue(validatorMirror, "value"));
        Class<? extends Validator> validatorClass = SafeReflectionUtil.forNameSubclass(validatorClassName, Validator.class);
        if (validatorClass == null) {
            throw new IllegalStateException("Failed to get Validator class for name [%s]".formatted(validatorClassName));
        }

        try {
            return UnsafeReflectionUtil.tryConstruct(validatorClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate validator [%s]".formatted(validatedAnnotation), e);
        }
    }
}
