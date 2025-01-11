package dev.jackraidenph.libraomni.annotation.classprocessing.processor;

import dev.jackraidenph.libraomni.annotation.classprocessing.processor.base.AbstractPredicateProcessor;
import dev.jackraidenph.libraomni.annotation.instance.Register;

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
    public List<PredicateWithDescription<Element>> getPredicatesAndDescriptions() {
        return List.of(
                PredicateWithDescription.MUST_BE_ON_CLASS,
                PredicateWithDescription.mustExtend(
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
