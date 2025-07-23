package dev.jackraidenph.libraomni.annotation;

import java.lang.annotation.*;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delegate {
    Class<? extends Annotation> annotation();

    String attribute();

    Class<? extends Function<Object, Object>> transformer() default NoOpTransformer.class;

    class NoOpTransformer implements Function<Object, Object> {
        @Override
        public Object apply(Object o) {
            return o;
        }
    }
}
