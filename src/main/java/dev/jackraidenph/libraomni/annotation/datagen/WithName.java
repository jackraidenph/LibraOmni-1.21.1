package dev.jackraidenph.libraomni.annotation.datagen;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WithName {
    String value();
}
