package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.IncompatibleWith;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.annotation.value.ValidatedExpression;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.validation.Validator;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

final class ValidateAnnotationsTask implements CompilationTask {

    @Override
    public void processRound(ModIdGetter modLocator, ProcessingContext processingContext) {
        Messager messager = processingContext.processingEnvironment().getMessager();

        messager.printNote("---VALIDATING ANNOTATIONS---");

        Set<TypeElement> annotations = getTypeElementAnnotations(processingContext.roundEnvironment());

        Set<TypeElement> toCheckIncompatible = getRequiringIncompatibleCheck(annotations);
        for (TypeElement checkIncompatible : toCheckIncompatible) {
            Set<? extends Element> annotatedElements = annotatedWith(checkIncompatible, processingContext.roundEnvironment());
            for (Element e : annotatedElements) {
                checkIncompatiblePresence(e, checkIncompatible, processingContext.processingEnvironment().getTypeUtils());
            }
        }

        Set<TypeElement> validatedAnnotations = getRequiringValidation(annotations);
        for (TypeElement validatedAnnotation : validatedAnnotations) {
            Set<? extends Element> annotatedElements = annotatedWith(validatedAnnotation, processingContext.roundEnvironment());

            Validated singular = validatedAnnotation.getAnnotation(Validated.class);
            ValidatedExpression expression = validatedAnnotation.getAnnotation(ValidatedExpression.class);

            if (singular != null && expression != null) {
                throw new IllegalStateException("Either @Validated or @ValidatedExpression is permitted, but not both");
            }

            Elements elements = processingContext.processingEnvironment().getElementUtils();
            messager.printNote("Found validation constraint [%s] for [@%s]".formatted(
                    singular != null ? validatedToString(singular, elements) : expressionToString(expression, elements),
                    validatedAnnotation.getSimpleName()
            ));

            if (singular != null) {
                processSingular(singular, validatedAnnotation, annotatedElements, processingContext);
            } else {
                processExpression(expression, validatedAnnotation, annotatedElements, processingContext);
            }
        }

        messager.printNote("---ANNOTATIONS SUCCESSFULLY VALIDATED---");
    }

    private void checkIncompatiblePresence(Element e, TypeElement annotation, Types types) {
        IncompatibleWith incompatibleWith = annotation.getAnnotation(IncompatibleWith.class);
        if (incompatibleWith == null) {
            return;
        }

        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            for (TypeMirror typeMirror : AnnotationMirrorUtil.mirrorClassArray(incompatibleWith::value)) {
                TypeMirror mirrorToCheck = AnnotationMirrorUtil.toTypeElement(annotationMirror).asType();
                if (types.isSameType(mirrorToCheck, typeMirror)) {
                    throw new AnnotationValidationException("Annotations [%s] and [%s] are incompatible, encountered on element [%s]".formatted(annotation, typeMirror, e));
                }
            }
        }
    }

    private void processSingular(
            Validated singular,
            TypeElement validatedAnnotation,
            Collection<? extends Element> toValidate,
            ProcessingContext processingContext
    ) {
        Elements elements = processingContext.processingEnvironment().getElementUtils();

        Validator validator = validatorFromAnnotation(singular, elements);
        List<String> args = List.of(singular.args());

        for (Element e : toValidate) {
            validate(e, validatedAnnotation, validator, args, processingContext);
        }
    }

    private void processExpression(
            ValidatedExpression expression,
            TypeElement validatedAnnotation,
            Collection<? extends Element> toValidate,
            ProcessingContext processingContext
    ) {
        Elements elements = processingContext.processingEnvironment().getElementUtils();

        List<Map.Entry<Validator, List<String>>> validatorsWithArgs = new ArrayList<>();
        for (Validated atom : expression.value()) {
            Validator validator = validatorFromAnnotation(atom, elements);
            List<String> args = List.of(atom.args());
            validatorsWithArgs.add(Map.entry(validator, args));
        }

        for (Element e : toValidate) {
            switch (expression.type()) {
                case OR -> validateOr(e, validatedAnnotation, validatorsWithArgs, processingContext);
                case AND -> validateAnd(e, validatedAnnotation, validatorsWithArgs, processingContext);
            }
        }
    }

    private void validateOr(
            Element element,
            TypeElement validatedAnnotation,
            List<Map.Entry<Validator, List<String>>> validatorsAndArgs,
            ProcessingContext processingContext
    ) {
        StringJoiner exceptionJoiner = new StringJoiner("\nOR\n", "\n", "");
        for (Map.Entry<Validator, List<String>> e : validatorsAndArgs) {
            try {
                validate(element, validatedAnnotation, e.getKey(), e.getValue(), processingContext);
                //Won't get called unless validate doesn't throw, which means successful validation
                return;
            } catch (AnnotationValidationException validationException) {
                exceptionJoiner.add(validationException.getLocalizedMessage());
            }
        }
        throw new AnnotationValidationException("None of validators in ValidatedExpression clause validated successfully, element [%s], annotation [@%s]: %s".formatted(element, validatedAnnotation.getSimpleName(), exceptionJoiner));
    }

    private void validateAnd(
            Element element,
            TypeElement validatedAnnotation,
            List<Map.Entry<Validator, List<String>>> validatorsAndArgs,
            ProcessingContext processingContext
    ) {
        for (Map.Entry<Validator, List<String>> e : validatorsAndArgs) {
            validate(element, validatedAnnotation, e.getKey(), e.getValue(), processingContext);
        }
    }

    private void validate(Element validatedElement,
                          TypeElement validatedAnnotation,
                          Validator validator,
                          List<String> args,
                          ProcessingContext processingContext
    ) {
        if (validatedElement.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
            return;
        }

        try {
            validator.test(validatedElement, validatedAnnotation, args, processingContext);
        } catch (AnnotationValidationException validationException) {
            throw validationException;
        } catch (Exception otherException) {
            throw new AnnotationValidationException(validatedAnnotation, validatedAnnotation, otherException);
        }

        processingContext.processingEnvironment().getMessager().printNote("[%s] was validated with no problems".formatted(validatedElement));
    }

    private Set<? extends Element> annotatedWith(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        return roundEnvironment
                .getElementsAnnotatedWith(typeElement)
                .stream()
                .filter(e -> !(e.getKind().equals(ElementKind.ANNOTATION_TYPE)))
                .collect(Collectors.toSet());
    }

    private Set<TypeElement> getTypeElementAnnotations(RoundEnvironment roundEnvironment) {
        return roundEnvironment
                .getRootElements()
                .stream()
                .map(Element::getAnnotationMirrors)
                .flatMap(List::stream)
                .map(AnnotationMirrorUtil::toTypeElement)
                .collect(Collectors.toSet());
    }

    private Set<TypeElement> getRequiringValidation(Set<TypeElement> annotations) {
        return annotations.stream()
                .filter(e -> e.getAnnotation(Validated.class) != null
                        || e.getAnnotation(ValidatedExpression.class) != null
                )
                .collect(Collectors.toSet());
    }

    private Set<TypeElement> getRequiringIncompatibleCheck(Set<TypeElement> annotations) {
        return annotations.stream().filter(e -> e.getAnnotation(IncompatibleWith.class) != null).collect(Collectors.toSet());
    }


    private Validator validatorFromAnnotation(
            Validated validatorAnnotation,
            Elements elementUtil
    ) {
        try {
            Class<?> ignored = validatorAnnotation.value();
        } catch (MirroredTypeException e) {
            TypeElement validatorTypeMirror = (TypeElement) ((DeclaredType) e.getTypeMirror()).asElement();
            String mirrorName = elementUtil.getBinaryName(validatorTypeMirror).toString();
            Class<? extends Validator> validatorClass = SafeReflectionUtil.forNameSubclass(mirrorName, Validator.class);
            if (validatorClass == null) {
                throw new IllegalStateException("Compile-time class for name [%s] not found".formatted(mirrorName));
            }
            return UnsafeReflectionUtil.tryConstruct(validatorClass);
        }
        throw new IllegalStateException("Failed to get validator from [%s]".formatted(validatorAnnotation));
    }

    private String expressionToString(ValidatedExpression expression, Elements elements) {
        StringJoiner atomJoiner = new StringJoiner(' ' + expression.type().name() + ' ');
        for (Validated term : expression.value()) {
            atomJoiner.add(validatedToString(term, elements));
        }
        return atomJoiner.toString();
    }

    private String validatedToString(Validated validated, Elements elements) {
        StringBuilder builder = new StringBuilder();
        StringJoiner argJoiner = new StringJoiner(", ", "(", ")");
        for (String arg : validated.args()) {
            argJoiner.add(arg);
        }
        return builder.append(validatorFromAnnotation(validated, elements).getClass().getSimpleName()).append(argJoiner).toString();
    }
}
