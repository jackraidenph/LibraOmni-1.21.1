package dev.jackraidenph.libraomni.annotation.value;

import java.lang.annotation.Target;

@Target({})
public @interface KeyValue {
    String key();

    String value();
}