package dev.jackraidenph.libraomni.experimental;

import dev.jackraidenph.libraomni.LibraOmni;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ForwardingLoggerWrapper implements LoggerWrapper {

    private Logger logger;

    private ForwardingLoggerWrapper(Logger logger) {
        setLogger(logger);
    }

    @Override
    public void setLogger(Logger logger) {
        //Forbid to set logger around unless black magic is happening
        if (!BlackMagicBootstrap.isBlackMagicActive()) {
            return;
        }
        this.logger = logger;
    }

    public static Logger make(Logger logger) {
        ForwardingLoggerWrapper wrapper = new ForwardingLoggerWrapper(logger);

        return (Logger) Proxy.newProxyInstance(
                LibraOmni.class.getClassLoader(),
                new Class[]{LoggerWrapper.class, Logger.class},
                new LoggerInvocationHelper(wrapper)
        );
    }

    private record LoggerInvocationHelper(ForwardingLoggerWrapper wrapper) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("setLogger")) {
                wrapper.setLogger((Logger) args[0]);
                return null;
            }

            Logger original = wrapper.logger;
            method.setAccessible(true);
            return method.invoke(original, args);
        }
    }

}
