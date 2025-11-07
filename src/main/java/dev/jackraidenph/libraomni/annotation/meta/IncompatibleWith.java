package dev.jackraidenph.libraomni.annotation.meta;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
public @interface IncompatibleWith {
    Class<? extends Annotation>[] value();
}
