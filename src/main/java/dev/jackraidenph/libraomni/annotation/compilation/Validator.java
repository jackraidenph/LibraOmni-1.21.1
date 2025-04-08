package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public interface Validator {
    boolean test(Element element, Messager messager);
}
