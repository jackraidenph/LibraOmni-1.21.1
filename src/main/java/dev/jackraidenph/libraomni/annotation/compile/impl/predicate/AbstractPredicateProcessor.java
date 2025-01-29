package dev.jackraidenph.libraomni.annotation.compile.impl.predicate;

import dev.jackraidenph.libraomni.annotation.compile.impl.AbstractCompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.CompilationPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.util.List;

public abstract class AbstractPredicateProcessor extends AbstractCompileTimeProcessor {

    public AbstractPredicateProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            for (Element e : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                for (CompilationPredicate<Element> predicate : this.getPredicatesAndDescriptions()) {
                    if (!predicate.predicate().test(e)) {
                        this.getProcessingEnvironment().getMessager().printError("""
                                Annotation conditions are not satisfied
                                Annotation: %s
                                Details: %s
                                """.formatted(annotation.toString(), predicate.description()), e);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<CompilationPredicate<Element>> getPredicatesAndDescriptions() {
        return List.of();
    }

}
