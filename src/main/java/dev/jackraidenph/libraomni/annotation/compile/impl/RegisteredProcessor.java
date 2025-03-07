package dev.jackraidenph.libraomni.annotation.compile.impl;

import dev.jackraidenph.libraomni.annotation.compile.util.CompilationPredicates;
import dev.jackraidenph.libraomni.annotation.impl.Registered;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public class RegisteredProcessor extends AbstractCompilationProcessor {

    public RegisteredProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public Set<CompilationPredicate<Element>> predicates() {
        return Set.of(
                CompilationPredicates.MUST_BE_ON_CLASS,
                CompilationPredicates.mustExtend(
                        "net.minecraft.world.level.block.Block"
                )
        );
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(
                Registered.class
        );
    }
}
