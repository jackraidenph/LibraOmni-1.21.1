package dev.jackraidenph.libraomni.data.reflect;

import javax.lang.model.element.ElementKind;
import java.lang.reflect.AnnotatedElement;

public interface AnnotatedReflectionData<T extends AnnotatedElement> {

    ElementKind kind();

    T construct();
}
