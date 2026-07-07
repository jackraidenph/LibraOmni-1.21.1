package dev.jackraidenph.libraomni.annotation.meta;

public @interface ValidatedExpression {

    Type type();

    Validated[] value();

    enum Type {
        OR,
        AND
    }
}
