package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;

public interface Validator {
    void test(Element element, TypeElement validatedAnnotation, @Nullable List<String> args, ProcessingContext processingContext);
}
