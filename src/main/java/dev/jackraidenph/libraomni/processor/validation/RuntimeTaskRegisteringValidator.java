package dev.jackraidenph.libraomni.processor.validation;

import dev.jackraidenph.libraomni.reflect.RuntimeTask;
import org.jetbrains.annotations.NotNull;

public class RuntimeTaskRegisteringValidator extends ExtensionValidator {
    @Override
    protected @NotNull Class<?> mustImplement() {
        return RuntimeTask.class;
    }
}
