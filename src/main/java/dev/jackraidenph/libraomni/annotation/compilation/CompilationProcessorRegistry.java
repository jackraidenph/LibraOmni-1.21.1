package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompilationProcessorRegistry {

    private static final Set<Function<ProcessingEnvironment, CompilationProcessor>> PROCESSORS_REGISTRY = new HashSet<>();

    static {
        registerAll(Set.of(
                MetadataProcessor::new,
                ValidationProcessor::new
        ));
    }

    private static void registerAll(Set<Function<ProcessingEnvironment, CompilationProcessor>> suppliers) {
        for (Function<ProcessingEnvironment, CompilationProcessor> s : suppliers) {
            register(s);
        }
    }

    private static void register(Function<ProcessingEnvironment, CompilationProcessor> processorSupplier) {
        Function<ProcessingEnvironment, CompilationProcessor> logCompose = processorSupplier.andThen(
                compilationProcessor -> {
                    compilationProcessor.processingEnvironment().getMessager().printNote(
                            "Registered " + compilationProcessor.getClass().getSimpleName() + " for compilation processing"
                    );
                    return compilationProcessor;
                }
        );
        PROCESSORS_REGISTRY.add(logCompose);
    }

    static Set<CompilationProcessor> instantiate(ProcessingEnvironment environment) {
        return PROCESSORS_REGISTRY.stream()
                .map(f -> f.apply(environment))
                .collect(Collectors.toSet());
    }
}
