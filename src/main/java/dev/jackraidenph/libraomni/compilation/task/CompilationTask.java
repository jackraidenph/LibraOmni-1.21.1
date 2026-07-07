package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import java.lang.annotation.Annotation;
import java.util.Set;

interface CompilationTask {

    default boolean processStage(ModIdGetter modLocator, ProcessingContext context) {
        boolean finish = context.roundEnvironment().processingOver();

        //Means that the called method is empty, no need to call it at all
        if (!SafeReflectionUtil.isIntefaceMethodOverriden(
                this.getClass(),
                finish ? "finish" : "processRound",
                ModIdGetter.class,
                ProcessingContext.class
        )) {
            return false;
        }

        if (finish) {
            finish(modLocator, context);
        } else {
            processRound(modLocator, context);
        }

        return true;
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
