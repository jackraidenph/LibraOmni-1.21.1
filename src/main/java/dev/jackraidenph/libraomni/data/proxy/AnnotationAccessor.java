package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;

public interface AnnotationAccessor<T> extends AnnotatedElement {
    AnnotatedElement annotatedObject();
}
