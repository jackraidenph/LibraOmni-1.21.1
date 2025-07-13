package dev.jackraidenph.libraomni.processor.validation;

import javax.annotation.Nonnull;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public abstract class ExtensionValidator implements Validator {

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
        Messager messager = processingEnvironment.getMessager();

        String toExtendOrImplement = classNameToValidateAgainst();

        boolean implementsRuntimeProcessor = ValidationUtils.elementImplementsOrExtendsAny(
                element,
                toExtendOrImplement
        );

        if (!implementsRuntimeProcessor) {
            messager.printError(element.getSimpleName().toString() + " must implement or extend " + toExtendOrImplement);
        }

        return implementsRuntimeProcessor;
    }

    @Nonnull
    protected String classNameToValidateAgainst() {
        return Object.class.getName();
    }
}
