package dev.jackraidenph.libraomni.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delegate {
    Class<? extends Annotation> annotation();

    String attribute();
}
