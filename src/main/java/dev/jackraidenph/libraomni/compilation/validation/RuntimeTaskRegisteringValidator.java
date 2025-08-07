package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.runtime.task.RuntimeTask;
import org.jetbrains.annotations.NotNull;

public class RuntimeTaskRegisteringValidator extends AssignabilityValidator {
    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return RuntimeTask.class.getName();
    }
}
