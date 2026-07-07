package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@ArbitraryItemModelData
@Composed
public @interface BlockItemModel {

    @Delegate(annotation = ArbitraryItemModelData.class, attribute = "parentModel")
    String value() default "";
}
