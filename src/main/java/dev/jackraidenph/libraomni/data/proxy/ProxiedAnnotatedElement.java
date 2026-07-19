package dev.jackraidenph.libraomni.data.proxy;

import java.lang.reflect.AnnotatedElement;

/**
 * This interface has to exist, because, unlike java.lang.model, reflective elements like Class, Method, etc., are not interface-based, but are concrete classes
 * this means that, unlike with java.lang.model.Element, instanceof checks can't be performend on constructed proxies against model interfaces.
 * Construction of dynamic subclass-proxies also proved to fail, because java.reflect classes are final and can't be subclassed, even with instrumentation
 */
public interface ProxiedAnnotatedElement extends AnnotatedElement {
    AnnotatedElement proxiedElement();
}
