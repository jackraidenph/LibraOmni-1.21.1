package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractCompilationProcessor implements CompilationProcessor {

    private final ProcessingEnvironment processingEnvironment;

    public AbstractCompilationProcessor(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    public void processRound(RoundEnvironment roundEnvironment) {
    }

    @Override
    public void finish(RoundEnvironment roundEnvironment) {
    }

    public Set<CompilationPredicate<Element>> predicates() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of();
    }

    private boolean processPredicates(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.supportedAnnotations()) {
            for (Element e : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                for (CompilationPredicate<Element> predicate : this.predicates()) {
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

    @Override
    public final void checkAndProcessRound(RoundEnvironment roundEnvironment) {
        if (!this.processPredicates(roundEnvironment)) {
            throw new IllegalStateException("Predicate check failed");
        }
        this.processRound(roundEnvironment);
    }

    @Override
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnvironment;
    }

    public record CompilationPredicate<T>(Predicate<T> predicate, String description) {
    }
}
