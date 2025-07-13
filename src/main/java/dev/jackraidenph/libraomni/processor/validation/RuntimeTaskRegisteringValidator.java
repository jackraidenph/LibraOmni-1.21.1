package dev.jackraidenph.libraomni.processor.validation;

import dev.jackraidenph.libraomni.reflect.RuntimeTask;
import org.jetbrains.annotations.NotNull;

public class RuntimeTaskRegisteringValidator extends ExtensionValidator {
    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return RuntimeTask.class.getName();
    }
}
