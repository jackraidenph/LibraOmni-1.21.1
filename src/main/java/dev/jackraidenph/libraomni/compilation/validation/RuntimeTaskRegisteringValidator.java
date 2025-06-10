package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.reflect.RuntimeTask;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public class RuntimeTaskRegisteringValidator implements Validator {
    @Override
    public boolean test(Element element, Messager messager) {
        String runtimeProcessorClassName = RuntimeTask.class.getName();
        boolean implementsRuntimeProcessor = ValidationUtils.elementImplementsAny(element, runtimeProcessorClassName);
        if (!implementsRuntimeProcessor) {
            messager.printError(element.getSimpleName().toString() + " must implement " + runtimeProcessorClassName);
        }
        return implementsRuntimeProcessor;
    }
}
