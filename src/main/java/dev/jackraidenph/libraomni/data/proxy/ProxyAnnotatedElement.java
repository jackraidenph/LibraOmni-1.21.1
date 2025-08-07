package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;

public interface ProxyAnnotatedElement extends AnnotatedElement {
    AnnotatedElement original();
}
