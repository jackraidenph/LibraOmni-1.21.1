package dev.jackraidenph.libraomni.annotation.service;

import dev.jackraidenph.libraomni.compilation.validation.RuntimeTaskRegisteringValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Validated(RuntimeTaskRegisteringValidator.class)
public @interface IsRuntimeTask {

}
