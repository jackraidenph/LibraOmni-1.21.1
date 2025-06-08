package dev.jackraidenph.libraomni.annotation.compilation.validation;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public interface Validator {
    boolean test(Element element, Messager messager);
}
