package dev.jackraidenph.libraomni.annotation.datagen;


import dev.jackraidenph.libraomni.annotation.value.KeyValue;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(BlockModels.class)
public @interface GeneratesBlockModelData {
    KeyValue[] value() default {};

    String parentModel() default "block/cube";
}
