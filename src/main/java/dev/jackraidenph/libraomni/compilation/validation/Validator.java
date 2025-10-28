package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.util.List;

public interface Validator {
    boolean test(Element element, @Nullable List<String> args, ProcessingContext processingContext);
}
