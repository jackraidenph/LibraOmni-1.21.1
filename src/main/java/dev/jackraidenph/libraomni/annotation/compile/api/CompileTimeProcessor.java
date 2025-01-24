package dev.jackraidenph.libraomni.annotation.compile.api;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.lang.annotation.Annotation;
import java.util.Set;

public interface CompileTimeProcessor {

    boolean onRound(RoundEnvironment roundEnvironment);

    boolean onFinish(RoundEnvironment roundEnvironment);

    Set<Class<? extends Annotation>> getSupportedAnnotationClasses();

    ProcessingEnvironment getProcessingEnvironment();

    static PackageElement packageOf(ProcessingEnvironment processingEnvironment, Element element) {
        return processingEnvironment.getElementUtils().getPackageOf(element);
    }
}
