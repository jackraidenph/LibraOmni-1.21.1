package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.util.ElementUtil;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.List;

public class TypesValidator implements Validator {

    @Override
    public void test(Element validatedElement, TypeElement validatedAnnotation, List<String> args, ProcessingContext processingContext) {
        ProcessingEnvironment processingEnvironment = processingContext.processingEnvironment();

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("%s takes in at least 1 argument, a type an element must be assignable to".formatted(this.getClass().getSimpleName()));
        }

        String toExtendOrImplement = args.getFirst();
        TypeElement typeElement = ElementUtil.getReturnTypeElement(validatedElement);


        if (!ElementUtil.isAssignableToAny(
                typeElement,
                processingEnvironment.getElementUtils(),
                processingEnvironment.getTypeUtils(),
                toExtendOrImplement
        )) {
            throw new AnnotationValidationException(
                    "[%s] must be assignable to any of [%s], got [%s]".formatted(validatedElement, toExtendOrImplement, typeElement)
            );
        }
    }

    @Override
    public String toString(@Nullable List<String> args, Elements elements) {
        return "Element must be either of types " + args;
    }
}
