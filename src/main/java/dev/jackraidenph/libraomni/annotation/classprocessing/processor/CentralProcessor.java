package dev.jackraidenph.libraomni.annotation.classprocessing.processor;

import dev.jackraidenph.libraomni.annotation.classprocessing.processor.base.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.classprocessing.serialization.ReflectionCachingHelper;
import dev.jackraidenph.libraomni.annotation.classprocessing.serialization.SerializationHelper;
import dev.jackraidenph.libraomni.annotation.instance.Register;

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

public class CentralProcessor extends AbstractProcessor {

    List<CompileTimeProcessor> processors = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        SerializationHelper serializationHelper = new SerializationHelper(ReflectionCachingHelper.INSTANCE);
        ScanRootProcessor scanRootProcessor = new ScanRootProcessor(processingEnv);

        this.addProcessors(
                scanRootProcessor,
                new RegisterPredicateProcessor(processingEnv),
                new ReferenceMapCreationProcessor(
                        processingEnv,
                        serializationHelper,
                        scanRootProcessor
                )
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnv.getMessager();

        for (CompileTimeProcessor compileTimeProcessor : this.processors) {
            if (roundEnvironment.processingOver()) {
                messager.printNote("Finishing " + compileTimeProcessor);
                boolean successfulFinish = compileTimeProcessor.onFinish(roundEnvironment);
                if (!successfulFinish) {
                    messager.printError("There was an error finishing either of compile-time processors");
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compileTimeProcessor);
            boolean successfulRound = compileTimeProcessor.onRound(roundEnvironment);
            if (!successfulRound) {
                messager.printError("There was an error during a round of either of compile-time processors");
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
        return Set.of(
                Register.class
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> qualifiers = new HashSet<>();
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            qualifiers.add(annotation.getName());
        }
        return Set.of(Register.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }
}
