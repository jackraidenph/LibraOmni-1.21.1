package dev.jackraidenph.libraomni.annotation.datagen;


import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.info.Internal;
import dev.jackraidenph.libraomni.annotation.validation.ValidatedBlockAnnotation;
import dev.jackraidenph.libraomni.annotation.value.StringPair;

import java.lang.annotation.*;

@Internal
@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(ArbitraryBlockModelData.Container.class)

@ValidatedBlockAnnotation
public @interface ArbitraryBlockModelData {
    StringPair[] value() default {};

    String parentModel() default "block/cube";

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @interface Container {
        ArbitraryBlockModelData[] value();
    }
}
