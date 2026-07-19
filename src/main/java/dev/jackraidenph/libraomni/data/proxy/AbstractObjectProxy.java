package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import java.lang.reflect.Method;

public abstract class AbstractObjectProxy<T> extends AbstractInterceptorProxy {

    protected final T proxiedObject;

    protected AbstractObjectProxy(T proxiedObject) {
        super();
        this.proxiedObject = proxiedObject;
    }

    public T getProxiedObject() {
        return proxiedObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (hasInterceptorsFor(method)) {
            return super.invoke(proxy, method, args);
        }

        return UnsafeReflectionUtil.getMethodValue(method, proxiedObject, args);
    }
}
