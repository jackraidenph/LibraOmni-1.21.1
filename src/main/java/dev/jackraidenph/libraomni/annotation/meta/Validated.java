package dev.jackraidenph.libraomni.annotation.meta;

import dev.jackraidenph.libraomni.compilation.validation.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Validated {
    Class<? extends Validator> value();
}
