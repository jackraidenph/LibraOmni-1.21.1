package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.service.Composed;
import dev.jackraidenph.libraomni.annotation.service.Delegate;
import dev.jackraidenph.libraomni.annotation.service.Id;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Composed
@Id
public @interface Registered {

    @Delegate(annotation = Id.class, attribute = "value")
    String value() default "";

    String propertiesId() default "";
}
