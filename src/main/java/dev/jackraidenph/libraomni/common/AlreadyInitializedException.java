package dev.jackraidenph.libraomni.common;

public class AlreadyInitializedException extends IllegalStateException {
    @Override
    public String getMessage() {
        return "Already initialized";
    }
}
