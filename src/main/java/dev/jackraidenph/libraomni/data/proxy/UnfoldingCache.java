package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;

import java.lang.annotation.Annotation;
import java.util.List;

public interface UnfoldingCache<T> {

    default void cache(List<? extends T> toCache) {
        for (T t : toCache) {
            Annotated annotated = annotated(t);
            UnfoldsInto unfoldInfo = annotated.getAnnotation(UnfoldsInto.class);

            if (unfoldInfo == null || unfoldInfo.retainSelf()) {
                save(t);
            }

            if (unfoldInfo != null) {
                List<? extends T> unfolded = unfold(t, unfoldInfo);
                cache(unfolded);
            }
        }
    }

    Annotated annotated(T object);

    List<? extends T> unfold(T toUnfold, UnfoldsInto unfoldInfo);

    void save(T toSave);

    @FunctionalInterface
    interface Annotated {
        <T extends Annotation> T getAnnotation(Class<T> annotationType);
    }
}
