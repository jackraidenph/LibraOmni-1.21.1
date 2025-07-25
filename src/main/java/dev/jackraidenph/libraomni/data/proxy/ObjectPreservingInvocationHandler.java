package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public abstract class ObjectPreservingInvocationHandler<T> implements InvocationHandler {

    protected final T original;

    public ObjectPreservingInvocationHandler(T original) {
        this.original = original;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return UnsafeReflectionUtil.getMethodValue(method, original, args);
    }
}
