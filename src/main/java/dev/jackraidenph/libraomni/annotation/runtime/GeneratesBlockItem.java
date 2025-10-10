package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.compilation.validation.BlockItemGenerationValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@NeedsRuntimeProcessing
@Validated(BlockItemGenerationValidator.class)
public @interface GeneratesBlockItem {
    /**
     * Properties ID
     */
    String propertiesId() default "";
}
