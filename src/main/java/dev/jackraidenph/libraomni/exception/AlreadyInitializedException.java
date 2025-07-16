package dev.jackraidenph.libraomni.exception;

public class AlreadyInitializedException extends IllegalStateException {
    @Override
    public String getMessage() {
        return "Already initialized";
    }
}
