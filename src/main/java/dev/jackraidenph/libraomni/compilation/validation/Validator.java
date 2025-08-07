package dev.jackraidenph.libraomni.compilation.validation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public interface Validator {
    boolean test(Element element, ProcessingEnvironment processingEnvironment);
}
