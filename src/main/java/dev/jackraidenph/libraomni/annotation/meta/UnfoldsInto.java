package dev.jackraidenph.libraomni.annotation.meta;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UnfoldsInto {
    Class<? extends Annotation>[] value();

    boolean retainSelf() default true;
}
