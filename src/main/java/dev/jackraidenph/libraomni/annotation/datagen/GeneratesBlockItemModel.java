package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@GeneratesItemModelData
@Composed
public @interface GeneratesBlockItemModel {

    @Delegate(annotation = GeneratesItemModelData.class, attribute = "parentModel")
    String value() default "";
}
