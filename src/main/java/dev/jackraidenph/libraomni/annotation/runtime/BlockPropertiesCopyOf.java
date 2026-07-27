package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.IncompatibleWith;
import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.validation.ValidatedBlockAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})

@ValidatedBlockAnnotation
@IncompatibleWith(BlockPropertiesByName.class)
public @interface BlockPropertiesCopyOf {
    String namespace() default "";

    String value();
}
