package dev.jackraidenph.libraomni.annotation.validation;

import dev.jackraidenph.libraomni.compilation.validation.Validator;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Validated {
    Class<? extends Validator> value();

    String[] args() default {};
}
