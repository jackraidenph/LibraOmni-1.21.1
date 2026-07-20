package dev.jackraidenph.libraomni.annotation.meta;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(InterceptorFor.Container.class)
public @interface InterceptorFor {
    String value();

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {
        InterceptorFor[] value();
    }
}
