package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;

public interface AnnotationAccessor extends AnnotatedElement {
    AnnotatedElement annotatedObject();
}
