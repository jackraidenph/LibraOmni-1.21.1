package dev.jackraidenph.libraomni.annotation.datagen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface GeneratesBlockStateData {
    String modelNamespace() default "";

    String modelName() default "";

    String value() default "";
}
