package dev.jackraidenph.libraomni.data.proxy;

import java.lang.annotation.Annotation;
import java.util.*;

public interface AnnotationAccessor<T> {
    Collection<Annotation> getAnnotations();

    T unwrap();
}
