package dev.jackraidenph.libraomni.annotation.compilation;

import dev.jackraidenph.libraomni.annotation.compilation.CompilationProcessorsManager.ModLocator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

interface CompilationProcessor {

    Collection<Resource> processRound(ModLocator modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);

    default Collection<Resource> finish(ModLocator modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        return Set.of();
    }

    //Every captured annotation is processed if empty
    default Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }
}
