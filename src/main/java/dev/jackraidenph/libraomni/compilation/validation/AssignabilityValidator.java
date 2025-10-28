package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.common.ElementUtil;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.List;

public class AssignabilityValidator implements Validator {

    @Override
    public boolean test(Element validatedElement, List<String> args, ProcessingEnvironment processingEnvironment) {
        Messager messager = processingEnvironment.getMessager();

        if (args == null || args.isEmpty()) {
            return true;
        }

        String toExtendOrImplement = args.getFirst();
        TypeElement typeElement = ElementUtil.getReturnTypeElement(validatedElement);
        boolean assignable = ElementUtil.isAssignableToAny(
                typeElement,
                processingEnvironment.getElementUtils(),
                processingEnvironment.getTypeUtils(),
                toExtendOrImplement
        );

        if (!assignable) {
            messager.printError("[%s] must be assignable to [%s], got [%s]".formatted(validatedElement, toExtendOrImplement, typeElement));
        }

        return assignable;
    }
}
