package dev.jackraidenph.libraomni.exception;

import dev.jackraidenph.libraomni.runtime.task.RuntimeTask;

public class DuplicateTaskException extends IllegalArgumentException {
    private final RuntimeTask duplicate;

    public DuplicateTaskException(RuntimeTask task) {
        this.duplicate = task;
    }

    @Override
    public String getMessage() {
        return duplicate.getClass().getSimpleName();
    }
}
