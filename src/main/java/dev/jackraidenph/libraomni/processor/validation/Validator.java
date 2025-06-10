package dev.jackraidenph.libraomni.processor.validation;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

public interface Validator {
    boolean test(Element element, Messager messager);
}
