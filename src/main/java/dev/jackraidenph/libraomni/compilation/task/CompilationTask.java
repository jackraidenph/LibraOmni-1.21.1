package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import java.lang.annotation.Annotation;
import java.util.Set;

interface CompilationTask {

    default boolean processStage(ProcessingContext context) {
        boolean finish = context.roundEnvironment().processingOver();

        //Means that the called method is empty, no need to call it at all
        if (!UnsafeReflectionUtil.isIntefaceMethodOverriden(
                this.getClass(),
                finish ? "finish" : "processRound",
                ProcessingContext.class
        )) {
            return false;
        }

        if (finish) {
            finish(context);
        } else {
            processRound(context);
        }

        return true;
    }

    default void processRound(ProcessingContext processingContext) {

    }

    default void finish(ProcessingContext processingContext) {

    }

    //Every captured annotation is processed if empty
    default Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }
}
