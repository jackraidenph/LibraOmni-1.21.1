package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.service.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.service.Validated;
import dev.jackraidenph.libraomni.processor.validation.AddToCreativeTabValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@NeedsRuntimeProcessing
@Validated(AddToCreativeTabValidator.class)
public @interface InCreativeTab {
    String namespace() default "minecraft";

    String value();
}
