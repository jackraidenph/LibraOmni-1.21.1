package dev.jackraidenph.libraomni.annotation.datagen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface InTags {
    String[] value();
}
