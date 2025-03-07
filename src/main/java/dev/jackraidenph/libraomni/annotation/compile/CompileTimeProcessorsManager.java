package dev.jackraidenph.libraomni.annotation.compile;

import dev.jackraidenph.libraomni.annotation.compile.api.CompilationProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.resource.AnnotationMapProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.RegisteredProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.ScanRootProcessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompileTimeProcessorsManager extends AbstractProcessor {

    List<CompilationProcessor> processors = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        ScanRootProcessor scanRootProcessor = new ScanRootProcessor(processingEnv);

        this.addProcessors(
                scanRootProcessor,
                new RegisteredProcessor(processingEnv),
                new AnnotationMapProcessor(
                        processingEnv,
                        scanRootProcessor
                )
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnv.getMessager();

        for (CompilationProcessor compilationProcessor : this.processors) {
            if (roundEnvironment.processingOver()) {
                messager.printNote("Finishing " + compilationProcessor + "...");
                boolean successfulFinish = compilationProcessor.finish(roundEnvironment);
                if (!successfulFinish) {
                    messager.printError("There was an error finishing either of compile processors");
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compilationProcessor + "...");
            boolean successfulRound = compilationProcessor.checkAndProcessRound(roundEnvironment);
            if (!successfulRound) {
                messager.printError("There was an error during a round of either of compile processors");
                return false;
            }
        }

        return true;
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
