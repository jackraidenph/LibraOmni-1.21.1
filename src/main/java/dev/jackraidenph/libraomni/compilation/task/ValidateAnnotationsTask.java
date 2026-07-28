package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.IncompatibleWith;
import dev.jackraidenph.libraomni.annotation.validation.Validated;
import dev.jackraidenph.libraomni.annotation.validation.ValidatedExpression;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.validation.Validator;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ElementUtil;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

final class ValidateAnnotationsTask implements CompilationTask {

    @Override
    public void processRound(ProcessingContext processingContext) {
        Messager messager = processingContext.processingEnvironment().getMessager();

        messager.printNote("---VALIDATING ANNOTATIONS---");

        Collection<TypeElement> annotations = ElementUtil.getAllAnnotationTypes(processingContext.roundEnvironment()).stream()
                .map(ProxyFactory::makeAnnotatedConstructProxy)
                .map(c -> (TypeElement) c)
                .toList();

        Set<TypeElement> requiringIncompatibleCheck = getRequiringIncompatibleCheck(annotations);
        for (TypeElement typeElement : requiringIncompatibleCheck) {
            Set<? extends Element> annotatedElements = elementsAnnotatedWith(typeElement, processingContext.roundEnvironment());
            for (Element e : annotatedElements) {
                checkIncompatiblePresence(e, typeElement, processingContext.processingEnvironment().getTypeUtils());
            }
        }

        Set<TypeElement> validatedAnnotations = getAnnotationsRequiringValidation(annotations);
        for (TypeElement annotationToValidate : validatedAnnotations) {
            Set<? extends Element> elementsToValidate = elementsAnnotatedWith(annotationToValidate, processingContext.roundEnvironment());
            validateElementsAgainstAnnotation(annotationToValidate, elementsToValidate, processingContext);
        }

        messager.printNote("---ANNOTATIONS VALIDATED SUCCESSFULLY---");
    }

    @Override
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        TypeElement proxy = (TypeElement) ProxyFactory.makeAnnotatedConstructProxy(AnnotationMirrorUtil.toTypeElement(mirror));
        return proxy.getAnnotation(Validated.class) != null || proxy.getAnnotation(ValidatedExpression.class) != null;
    }

    private static void validateElementsAgainstAnnotation(
            TypeElement annotationToValidate,
            Set<? extends Element> elementsToValidate,
            ProcessingContext processingContext
    ) {
        Messager messager = processingContext.processingEnvironment().getMessager();

        Validated constraintSingular = annotationToValidate.getAnnotation(Validated.class);
        ValidatedExpression constraintExpression = annotationToValidate.getAnnotation(ValidatedExpression.class);

        if (constraintSingular != null && constraintExpression != null) {
            throw new IllegalStateException("Either @Validated or @ValidatedExpression is permitted, but not both");
        }

        Elements elements = processingContext.processingEnvironment().getElementUtils();

        if (constraintSingular != null) {
            messager.printNote("Found validation constraint [%s] for [@%s]".formatted(
                    validatedToString(constraintSingular, elements),
                    annotationToValidate.getSimpleName()
            ));
            processSingular(constraintSingular, annotationToValidate, elementsToValidate, processingContext);
        } else if (constraintExpression != null) {
            messager.printNote("Found validation constraint expression [%s] for [@%s]".formatted(
                    expressionToString(constraintExpression, elements),
                    annotationToValidate.getSimpleName()
            ));
            processExpression(constraintExpression, annotationToValidate, elementsToValidate, processingContext);
        }
    }

    private static void checkIncompatiblePresence(Element element, TypeElement annotation, Types types) {
        IncompatibleWith incompatibleWithInfo = annotation.getAnnotation(IncompatibleWith.class);
        if (incompatibleWithInfo == null) {
            return;
        }

        List<? extends TypeMirror> incompatibleList = ElementUtil.mirrorClassArray(incompatibleWithInfo::value);

        ElementUtil.Javac.getAllAnnotationMirrors(element)
                .stream().map(AnnotationMirrorUtil::toTypeElement)
                .map(TypeElement::asType)
                .filter(typeToCheck -> containsTypeMirror(typeToCheck, incompatibleList, types))
                .findAny()
                .ifPresent(incompatible -> {
                    throw new AnnotationValidationException(
                            "Annotations [%s] and [%s] are incompatible, encountered on element [%s]".formatted(annotation, incompatible, element)
                    );
                });
    }

    private static boolean containsTypeMirror(TypeMirror typeMirror, Collection<? extends TypeMirror> collection, Types types) {
        return collection.stream().anyMatch(e -> types.isSameType(e, typeMirror));
    }

    private static void processSingular(
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

    private static void processExpression(
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

    private static void validateOr(
            Element element,
            TypeElement validatedAnnotation,
            List<Map.Entry<Validator, List<String>>> validatorsAndArgs,
            ProcessingContext processingContext
    ) {
        AnnotationValidationException accumulator = new AnnotationValidationException(
                """
                        None of validators in ValidatedExpression OR clause validated successfully, \
                        fix either of following exceptions, \
                        element [%s], annotation [@%s]\
                        """.formatted(element, validatedAnnotation.getSimpleName())
        );

        for (Map.Entry<Validator, List<String>> e : validatorsAndArgs) {
            try {
                validate(element, validatedAnnotation, e.getKey(), e.getValue(), processingContext);
                //Won't get called unless validate doesn't throw, which means successful validation
                return;
            } catch (AnnotationValidationException validationException) {
                accumulator.addSuppressed(validationException);
            }
        }
        throw accumulator;
    }

    private static void validateAnd(
            Element element,
            TypeElement validatedAnnotation,
            List<Map.Entry<Validator, List<String>>> validatorsAndArgs,
            ProcessingContext processingContext
    ) {
        for (Map.Entry<Validator, List<String>> e : validatorsAndArgs) {
            validate(element, validatedAnnotation, e.getKey(), e.getValue(), processingContext);
        }
    }

    private static void validate(Element validatedElement,
                                 TypeElement validatedAnnotation,
                                 Validator validator,
                                 List<String> args,
                                 ProcessingContext processingContext
    ) {
        try {
            validator.test(validatedElement, validatedAnnotation, args, processingContext);
        } catch (AnnotationValidationException validationException) {
            throw validationException;
        } catch (Exception otherException) {
            throw new AnnotationValidationException(validatedAnnotation, validatedAnnotation, otherException);
        }
    }

    private static Set<? extends Element> elementsAnnotatedWith(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        return roundEnvironment
                .getElementsAnnotatedWith(typeElement)
                .stream()
                .filter(e -> !(e.getKind().equals(ElementKind.ANNOTATION_TYPE)))
                .collect(Collectors.toSet());
    }

    private static Set<TypeElement> getAnnotationsRequiringValidation(Collection<TypeElement> annotations) {
        return annotations.stream()
                .filter(e -> e.getAnnotation(Validated.class) != null
                        || e.getAnnotation(ValidatedExpression.class) != null
                )
                .collect(Collectors.toSet());
    }

    private static Set<TypeElement> getRequiringIncompatibleCheck(Collection<TypeElement> annotations) {
        return annotations.stream().filter(e -> e.getAnnotation(IncompatibleWith.class) != null).collect(Collectors.toSet());
    }


    private static Validator validatorFromAnnotation(
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

    private static String expressionToString(ValidatedExpression expression, Elements elements) {
        StringJoiner atomJoiner = new StringJoiner(' ' + expression.type().name() + ' ');
        for (Validated term : expression.value()) {
            atomJoiner.add(validatedToString(term, elements));
        }
        return atomJoiner.toString();
    }

    private static String validatedToString(Validated validated, Elements elements) {
        return validatorFromAnnotation(validated, elements).toString(List.of(validated.args()), elements);
    }
}
