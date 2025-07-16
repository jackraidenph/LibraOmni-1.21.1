package dev.jackraidenph.libraomni.processor.validation;

import javax.annotation.Nonnull;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

public abstract class AssignabilityValidator implements Validator {

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
        Messager messager = processingEnvironment.getMessager();

        String toExtendOrImplement = classNameToValidateAgainst();

        boolean implementsRuntimeProcessor = ValidationUtils.elementImplementsOrExtendsAny(element, toExtendOrImplement);

        if (!implementsRuntimeProcessor) {
            messager.printError(element.getSimpleName().toString() + " must be assignable to " + toExtendOrImplement);
        }

        return implementsRuntimeProcessor;
    }

    @Nonnull
    protected String classNameToValidateAgainst() {
        return Object.class.getName();
    }
}
