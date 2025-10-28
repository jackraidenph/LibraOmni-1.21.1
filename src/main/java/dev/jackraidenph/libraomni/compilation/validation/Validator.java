package dev.jackraidenph.libraomni.compilation.validation;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.List;

public interface Validator {
    boolean test(Element element, @Nullable List<String> args, ProcessingEnvironment processingEnvironment);
}
