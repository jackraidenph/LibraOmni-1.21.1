package dev.jackraidenph.libraomni.annotation.compilation;

import dev.jackraidenph.libraomni.annotation.Validated;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.Set;
import java.util.stream.Collectors;

class ValidationProcessor extends AbstractCompilationProcessor {
    public ValidationProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public void processRound(RoundEnvironment roundEnvironment) {
        Set<TypeElement> validatedAnnotations = roundEnvironment
                .getRootElements()
                .stream()
                .flatMap(e -> e.getAnnotationMirrors().stream())
                .map(am -> (TypeElement) am.getAnnotationType().asElement())
                .filter(e -> e.getAnnotation(Validated.class) != null)
                .collect(Collectors.toSet());

        for (TypeElement annotationElement : validatedAnnotations) {
            Validator validator = this.getValidatorForAnnotation(annotationElement);
            if (validator == null) {
                this.messager().printWarning("Failed to get validator from " + annotationElement);
                continue;
            }
            Set<? extends Element> toValidate = roundEnvironment.getElementsAnnotatedWith(annotationElement);

            for (Element e : toValidate) {
                if (!validator.test(e, this.messager())) {
                    this.messager().printError("Validation failed for element [" + e.getSimpleName().toString() + "]");
                }
            }
        }
    }

    private Validator getValidatorForAnnotation(TypeElement annotationElement) {
        Validated validatorAnnotation = annotationElement.getAnnotation(Validated.class);
        try {
            validatorAnnotation.value();
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            Element element = typeUtils().asElement(typeMirror);
            TypeElement typeElement = (TypeElement) element;
            return ValidatorFactory.INSTANCE.getOrCreate(typeElement.getQualifiedName().toString());
        }

        return null;
    }
}
