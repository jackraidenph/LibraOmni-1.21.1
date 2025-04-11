package dev.jackraidenph.libraomni.annotation.compilation;

import net.neoforged.fml.common.Mod;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

public class CompilationProcessorsManager extends AbstractProcessor {

    private final Set<CompilationProcessor> processors = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.registerProcessors(processingEnv);
        super.init(processingEnv);
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
                    processorException.printStackTrace();
                    messager.printError("There was an error finishing a compile processor");
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compilationProcessor.getClass().getSimpleName() + "...");
            try {
                compilationProcessor.processRound(roundEnvironment);
            } catch (Exception processorException) {
                processorException.printStackTrace();
                messager.printError("There was an error during a round a compile processor");
                return false;
            }
        }

        return false;
    }

    private void registerProcessors(ProcessingEnvironment environment) {
        for (CompilationProcessor compilationProcessor : CompilationProcessorRegistry.instantiate(environment)) {
            if (this.processors.contains(compilationProcessor)) {
                throw new IllegalArgumentException("Duplicate processor");
            }
            this.processors.add(compilationProcessor);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                Mod.class.getName()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }
}
