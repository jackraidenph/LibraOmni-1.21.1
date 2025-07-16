package dev.jackraidenph.libraomni.processor.validation;

import dev.jackraidenph.libraomni.reflect.task.RuntimeTask;
import org.jetbrains.annotations.NotNull;

public class RuntimeTaskRegisteringValidator extends AssignabilityValidator {
    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return RuntimeTask.class.getName();
    }
}
