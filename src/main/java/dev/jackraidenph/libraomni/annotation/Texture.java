package dev.jackraidenph.libraomni.annotation;

import java.lang.annotation.Target;

@Target({})
public @interface Texture {
    String key();

    String path();
}