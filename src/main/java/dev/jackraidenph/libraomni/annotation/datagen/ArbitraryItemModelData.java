package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.info.Internal;
import dev.jackraidenph.libraomni.annotation.value.StringPair;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Internal
@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(ArbitraryItemModelData.Container.class)
public @interface ArbitraryItemModelData {
    StringPair[] value() default {};

    String parentModel() default "item/generated";

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @interface Container {
        ArbitraryItemModelData[] value();
    }
}
