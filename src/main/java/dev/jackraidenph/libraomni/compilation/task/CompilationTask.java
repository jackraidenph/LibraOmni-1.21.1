package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import java.lang.annotation.Annotation;
import java.util.Set;

interface CompilationTask {

    default void processStage(ModIdGetter modLocator, ProcessingContext context) {
        if (context.roundEnvironment().processingOver()) {
            finish(modLocator, context);
        } else {
            processRound(modLocator, context);
        }
    }

    default void processRound(ModIdGetter modLocator, ProcessingContext processingContext) {

    }

    default void finish(ModIdGetter modLocator, ProcessingContext processingContext) {

    }

    //Every captured annotation is processed if empty
    default Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }
}
