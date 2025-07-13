package dev.jackraidenph.libraomni.processor.validation;

import javax.annotation.Nonnull;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public abstract class ExtensionValidator implements Validator {

    @Override
    public boolean test(Element element, Messager messager) {
        Class<?> toImplement = mustImplement();

        boolean isInterface = toImplement.isInterface();

        String className = toImplement.getName();
        boolean implementsRuntimeProcessor = isInterface
                ? ValidationUtils.elementImplementsAny(element, className)
                : ValidationUtils.elementExtendsAny(element, className);

        if (!implementsRuntimeProcessor) {
            messager.printError(
                    element.getSimpleName().toString() +
                            "must " +
                            (isInterface ? "implement " : "extend ") +
                            className
            );
        }

        return implementsRuntimeProcessor;
    }

    @Nonnull
    protected Class<?> mustImplement() {
        return Object.class;
    }
}
