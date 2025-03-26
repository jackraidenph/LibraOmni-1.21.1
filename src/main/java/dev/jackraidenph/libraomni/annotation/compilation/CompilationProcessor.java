package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import java.lang.annotation.Annotation;
import java.util.Set;

public interface CompilationProcessor {

    boolean checkAndProcessRound(RoundEnvironment roundEnvironment);

    boolean finish(RoundEnvironment roundEnvironment);

    Set<Class<? extends Annotation>> supportedAnnotations();

    ProcessingEnvironment getProcessingEnvironment();
}
