package dev.jackraidenph.libraomni.annotation.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String value() default "";
}
