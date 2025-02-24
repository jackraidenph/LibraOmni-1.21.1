package dev.jackraidenph.libraomni.annotation.compile;

import dev.jackraidenph.libraomni.annotation.compile.api.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.AnnotationMapCreationProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.RegisteredProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.AnnotationScanRootProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;

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

    List<CompileTimeProcessor> processors = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        AnnotationScanRootProcessor annotationScanRootProcessor = new AnnotationScanRootProcessor(processingEnv);

        this.addProcessors(
                annotationScanRootProcessor,
                new RegisteredProcessor(processingEnv),
                new AnnotationMapCreationProcessor(
                        processingEnv,
                        annotationScanRootProcessor
                )
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnv.getMessager();

        for (CompileTimeProcessor compileTimeProcessor : this.processors) {
            if (roundEnvironment.processingOver()) {
                messager.printNote("Finishing " + compileTimeProcessor + "...");
                boolean successfulFinish = compileTimeProcessor.onFinish(roundEnvironment);
                if (!successfulFinish) {
                    messager.printError("There was an error finishing either of compile processors");
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compileTimeProcessor + "...");
            boolean successfulRound = compileTimeProcessor.onRound(roundEnvironment);
            if (!successfulRound) {
                messager.printError("There was an error during a round of either of compile processors");
                return false;
            }
        }

        return true;
    }

    private void addProcessors(CompileTimeProcessor... processors) {
        for (CompileTimeProcessor compileTimeProcessor : processors) {
            if (this.processors.contains(compileTimeProcessor)) {
                throw new IllegalArgumentException("Duplicate processor");
            }
            this.processors.add(compileTimeProcessor);
        }
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return this.processors.stream()
                .map(CompileTimeProcessor::getSupportedAnnotationClasses)
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
