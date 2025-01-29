package dev.jackraidenph.libraomni.annotation.compile.impl;

import dev.jackraidenph.libraomni.annotation.compile.util.CompilationPredicate;
import dev.jackraidenph.libraomni.annotation.impl.Register;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class RegisterPredicateProcessor extends AbstractPredicateProcessor {

    public RegisterPredicateProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public List<CompilationPredicate<Element>> getPredicatesAndDescriptions() {
        return List.of(
                CompilationPredicate.MUST_BE_ON_CLASS,
                CompilationPredicate.mustExtend(
                        "net.minecraft.world.level.block.Block"
                )
        );
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of(
                Register.class
        );
    }
}
