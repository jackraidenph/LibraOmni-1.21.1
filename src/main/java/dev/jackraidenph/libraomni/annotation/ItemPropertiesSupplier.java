package dev.jackraidenph.libraomni.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@NeedsRuntimeProcessing
public @interface ItemPropertiesSupplier {
}
