package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;

public interface ProxiedAnnotatedElement extends AnnotatedElement {
    AnnotatedElement proxiedElement();
}
