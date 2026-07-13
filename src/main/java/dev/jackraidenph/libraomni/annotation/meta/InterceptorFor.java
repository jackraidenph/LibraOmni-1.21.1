package dev.jackraidenph.libraomni.annotation.meta;

import dev.jackraidenph.libraomni.annotation.meta.InterceptorFor.InvokerForContainer;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(InvokerForContainer.class)
public @interface InterceptorFor {
    String value();

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface InvokerForContainer {
        InterceptorFor[] value();
    }
}
