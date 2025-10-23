package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.common.ElementUtil;

import javax.annotation.Nonnull;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

public abstract class AssignabilityValidator implements Validator {

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
        Messager messager = processingEnvironment.getMessager();

        String toExtendOrImplement = classNameToValidateAgainst();
        TypeElement typeElement = ElementUtil.getReturnType(element);
        boolean assignable = ElementUtil.isAssignableToAny(
                typeElement,
                processingEnvironment.getElementUtils(),
                processingEnvironment.getTypeUtils(),
                toExtendOrImplement
        );

        if (!assignable) {
            String name = element.getSimpleName().toString();
            String type = typeElement.getQualifiedName().toString();
            messager.printError("[%s] must be assignable to [%s], but got [%s]".formatted(name, toExtendOrImplement, type));
        }

        return assignable;
    }

    @Nonnull
    protected String classNameToValidateAgainst() {
        return Object.class.getName();
    }
}
