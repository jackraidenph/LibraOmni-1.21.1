package dev.jackraidenph.libraomni.compilation;

import dev.jackraidenph.libraomni.annotation.Validated;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.validation.Validator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Collectors;

class ValidateAnnotationsTask implements CompilationTask {

    @Override
    public Set<Resource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
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
                processingEnv.getMessager().printError("Failed to get validator from " + annotationElement);
                continue;
            }
            processingEnv.getMessager().printNote("Found validator [" + validator.getClass().getSimpleName() + "] for [" + annotationElement.getQualifiedName() + "]");

            Set<? extends Element> toValidate = roundEnv.getElementsAnnotatedWith(annotationElement);

            for (Element e : toValidate) {
                if (!validator.test(e, processingEnv.getMessager())) {
                    processingEnv.getMessager().printError("Validation failed for element [" + e.getSimpleName().toString() + "]");
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

            Validator validator = SafeReflectionUtil.tryConstruct(validatorClass);
            if (validator == null) {
                throw new IllegalStateException("Failed to construct Validator for [" + validatorClass.getSimpleName() + "]");
            }

            return validator;
        } catch (ClassCastException classCastException) {
            return null;
        }
    }
}
