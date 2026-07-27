package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.IncompatibleWith;
import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.validation.ValidatedItemAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})

@ValidatedItemAnnotation
@IncompatibleWith(ItemPropertiesByName.class)
public @interface ItemPropertiesCopyOf {
    String namespace() default "";

    String value();
}
