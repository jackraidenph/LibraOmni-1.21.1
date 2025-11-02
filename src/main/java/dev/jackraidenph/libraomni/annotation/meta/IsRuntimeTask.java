package dev.jackraidenph.libraomni.annotation.meta;


import dev.jackraidenph.libraomni.compilation.validation.TypesValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Validated(value = TypesValidator.class, args = "dev.jackraidenph.libraomni.runtime.task.RuntimeTask")
public @interface IsRuntimeTask {

}
