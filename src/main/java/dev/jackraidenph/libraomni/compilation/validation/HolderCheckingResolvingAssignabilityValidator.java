package dev.jackraidenph.libraomni.compilation.validation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public abstract class HolderCheckingResolvingAssignabilityValidator extends ResolvingAssignabilityValidator {

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
        String toExtendOrImplement = classNameToValidateAgainst();

        Element deferredHolderType = ValidationUtils.tryResolveDeferredHolder(element);
        if (deferredHolderType != null && ValidationUtils.elementImplementsOrExtendsAny(deferredHolderType, toExtendOrImplement)) {
            return true;
        }

        return super.test(element, processingEnvironment);
    }
}
