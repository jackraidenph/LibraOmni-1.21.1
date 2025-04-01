package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CompilationProcessorsManager extends AbstractProcessor {

    Set<CompilationProcessor> processors = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.addProcessors(
                new MetadataProcessor(processingEnv)
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnv.getMessager();

        for (CompilationProcessor compilationProcessor : this.processors) {
            if (roundEnvironment.processingOver()) {
                messager.printNote("Finishing " + compilationProcessor.getClass().getSimpleName() + "...");
                try {
                    compilationProcessor.finish(roundEnvironment);
                } catch (Exception processorException) {
                    messager.printError("There was an error finishing a compile processor");
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compilationProcessor.getClass().getSimpleName() + "...");
            try {
                compilationProcessor.processRound(roundEnvironment);
            } catch (Exception processorException) {
                messager.printError("There was an error during a round a compile processor");
                return false;
            }
        }

        return false;
    }

    private void addProcessors(CompilationProcessor... processors) {
        for (CompilationProcessor compilationProcessor : processors) {
            if (this.processors.contains(compilationProcessor)) {
                throw new IllegalArgumentException("Duplicate processor");
            }
            this.processors.add(compilationProcessor);
        }
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return this.processors.stream()
                .map(CompilationProcessor::supportedAnnotations)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> qualifiers = new HashSet<>();
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            qualifiers.add(annotation.getName());
        }
        return qualifiers;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }
}
