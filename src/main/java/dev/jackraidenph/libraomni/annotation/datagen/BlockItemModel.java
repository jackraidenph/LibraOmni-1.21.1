package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})

@UnfoldsInto(value = ArbitraryItemModelData.class, retainSelf = false)
public @interface BlockItemModel {

    @Replaces(in = ArbitraryItemModelData.class, attribute = "parentModel")
    String value() default "{mod_id}:block/{element_id}";
}
