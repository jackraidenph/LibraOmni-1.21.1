package dev.jackraidenph.libraomni.annotation.datagen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface BlockModels {
    GeneratesBlockModelData[] value();
}
