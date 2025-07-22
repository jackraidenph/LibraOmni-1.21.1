package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.InvocationHandler;

public abstract class ObjectPreservingInvocationHandler<T> implements InvocationHandler {

    protected final T original;

    public ObjectPreservingInvocationHandler(T original) {
        this.original = original;
    }

    public T unwrap() {
        return original;
    }
}
