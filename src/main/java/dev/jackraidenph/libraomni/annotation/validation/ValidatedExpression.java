package dev.jackraidenph.libraomni.annotation.validation;

import java.lang.annotation.Documented;

@Documented
public @interface ValidatedExpression {

    Type type();

    Validated[] value();

    enum Type {
        OR,
        AND
    }
}
