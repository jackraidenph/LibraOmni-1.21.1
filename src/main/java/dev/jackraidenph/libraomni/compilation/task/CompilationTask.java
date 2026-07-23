package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.data.proxy.compile.SyntheticAnnotationMirror;
import dev.jackraidenph.libraomni.util.ElementUtil;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public interface CompilationTask {

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

    default boolean requiresBlackMagicEnabled() {
        return false;
    }

    default int hashStructure(Element element, AnnotationMirror annotation) {
        String e = ElementUtil.stringIdentity(element);
        String a = SyntheticAnnotationMirror.stringIdentity(annotation, true);
        return e.hashCode() + a.hashCode();
    }

    default String className() {
        return this.getClass().getName();
    }
}
