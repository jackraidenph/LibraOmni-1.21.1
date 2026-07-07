package dev.jackraidenph.libraomni.annotation.value;

import java.lang.annotation.Target;

@Target({})
public @interface StringPair {
    String key();
    String value();
}