package dev.jackraidenph.libraomni.annotation.datagen;


import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.info.Internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Internal
@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface BlockStateModel {
    String model() default "{mod_id}:block/{element_id}";
}
