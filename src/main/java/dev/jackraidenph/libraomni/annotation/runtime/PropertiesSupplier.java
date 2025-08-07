package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.service.Composed;
import dev.jackraidenph.libraomni.annotation.service.NeedsRuntimeProcessing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Composed
public @interface PropertiesSupplier {
    String value();
}
