package dev.jackraidenph.libraomni.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registered {
    String value() default "";
}
