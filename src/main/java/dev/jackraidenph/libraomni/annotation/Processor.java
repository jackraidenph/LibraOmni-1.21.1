package dev.jackraidenph.libraomni.annotation;

import dev.jackraidenph.libraomni.annotation.compilation.RuntimeTaskValidator;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Validated(RuntimeTaskValidator.class)
public @interface Processor {
    Scope value();
}
