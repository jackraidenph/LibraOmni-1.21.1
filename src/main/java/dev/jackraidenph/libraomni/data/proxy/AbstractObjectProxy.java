package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public abstract class AbstractObjectProxy<T> implements InvocationHandler {

    protected final T proxiedObject;

    public AbstractObjectProxy(T proxiedObject) {
        this.proxiedObject = proxiedObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return UnsafeReflectionUtil.getMethodValue(method, proxiedObject, args);
    }
}
