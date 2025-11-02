package dev.jackraidenph.libraomni.annotation.value;

import dev.jackraidenph.libraomni.annotation.meta.Validated;

public @interface ValidatedExpression {

    Type type();

    Validated[] value();

    enum Type {
        OR,
        AND
    }
}
