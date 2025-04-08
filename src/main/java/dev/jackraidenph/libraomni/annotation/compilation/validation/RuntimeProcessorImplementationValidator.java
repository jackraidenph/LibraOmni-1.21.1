package dev.jackraidenph.libraomni.annotation.compilation.validation;

import dev.jackraidenph.libraomni.annotation.compilation.ValidationUtils;
import dev.jackraidenph.libraomni.annotation.compilation.Validator;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public class RuntimeProcessorImplementationValidator implements Validator {
    @Override
    public boolean test(Element element, Messager messager) {
        String runtimeProcessorClassName = RuntimeProcessor.class.getName();
        boolean implementsRuntimeProcessor = ValidationUtils.elementImplementsAny(element, runtimeProcessorClassName);
        if (!implementsRuntimeProcessor) {
            messager.printError(element.getSimpleName().toString() + " must implement " + runtimeProcessorClassName);
        }
        return implementsRuntimeProcessor;
    }
}
