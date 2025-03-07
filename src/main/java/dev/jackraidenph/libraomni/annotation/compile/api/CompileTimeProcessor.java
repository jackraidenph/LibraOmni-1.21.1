package dev.jackraidenph.libraomni.annotation.compile.api;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.lang.annotation.Annotation;
import java.util.Set;

public interface CompileTimeProcessor {

    boolean checkAndProcessRound(RoundEnvironment roundEnvironment);

    boolean finish(RoundEnvironment roundEnvironment);

    Set<Class<? extends Annotation>> supportedAnnotations();

    ProcessingEnvironment getProcessingEnvironment();

    static PackageElement packageOf(ProcessingEnvironment processingEnvironment, Element element) {
        return processingEnvironment.getElementUtils().getPackageOf(element);
    }
}
