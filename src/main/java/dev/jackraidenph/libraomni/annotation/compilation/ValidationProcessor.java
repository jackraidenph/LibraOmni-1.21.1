package dev.jackraidenph.libraomni.annotation.compilation;

import dev.jackraidenph.libraomni.annotation.Validated;
import dev.jackraidenph.libraomni.annotation.compilation.CompilationProcessorsManager.ModLocator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Set;
import java.util.stream.Collectors;

class ValidationProcessor implements CompilationProcessor {

    @Override
    public Set<Resource> processRound(ModLocator modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Set<TypeElement> validatedAnnotations = roundEnv
                .getRootElements()
                .stream()
                .flatMap(e -> e.getAnnotationMirrors().stream())
                .map(am -> (TypeElement) am.getAnnotationType().asElement())
                .filter(e -> e.getAnnotation(Validated.class) != null)
                .collect(Collectors.toSet());

        for (TypeElement annotationElement : validatedAnnotations) {
            Validator validator = this.getValidatorForAnnotation(annotationElement, processingEnv.getTypeUtils());
            if (validator == null) {
                processingEnv.getMessager().printWarning("Failed to get validator from " + annotationElement);
                continue;
            }
            Set<? extends Element> toValidate = roundEnv.getElementsAnnotatedWith(annotationElement);

            for (Element e : toValidate) {
                if (!validator.test(e, processingEnv.getMessager())) {
                    processingEnv.getMessager().printError("Validation failed for element [" + e.getSimpleName().toString() + "]");
                }
            }
        }

        return Set.of();
    }

    private Validator getValidatorForAnnotation(TypeElement annotationElement, Types types) {
        Validated validatorAnnotation = annotationElement.getAnnotation(Validated.class);
        try {
            validatorAnnotation.value();
            //This weird hack is the way to get a type mirror after an attempt to access a class
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            Element element = types.asElement(typeMirror);
            TypeElement typeElement = (TypeElement) element;
            return ValidatorFactory.INSTANCE.create(typeElement.getQualifiedName().toString());
        }

        return null;
    }
}
