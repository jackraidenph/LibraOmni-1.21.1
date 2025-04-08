package dev.jackraidenph.libraomni.annotation.compilation;

import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public class ProcessorAnnotationValidator implements Validator {
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
