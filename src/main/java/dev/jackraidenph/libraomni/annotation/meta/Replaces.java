package dev.jackraidenph.libraomni.annotation.meta;

import dev.jackraidenph.libraomni.util.TransformerUtil.NoOpTransformer;

import java.lang.annotation.*;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Replaces {
    String attribute();

    Class<? extends Annotation> in();

    Class<? extends Function<?, ?>> transformer() default NoOpTransformer.class;
}
