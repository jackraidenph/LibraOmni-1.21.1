package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.Messager;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompilationProcessorRegistry {

    private static final Set<Supplier<CompilationProcessor>> PROCESSORS_REGISTRY = new HashSet<>();

    static {
        registerAll(
                MetadataProcessor::new,
                ValidationProcessor::new
        );
    }

    @SafeVarargs
    static void registerAll(Supplier<CompilationProcessor>... suppliers) {
        for (Supplier<CompilationProcessor> s : suppliers) {
            register(s);
        }
    }

    static void register(Supplier<CompilationProcessor> processorSupplier) {
        PROCESSORS_REGISTRY.add(processorSupplier);
    }

    static Collection<CompilationProcessor> getAll(Messager messager) {
        return PROCESSORS_REGISTRY.stream()
                .map(Supplier::get)
                .peek(task -> messager.printNote("Registered " + task.getClass().getSimpleName() + " for processing"))
                .collect(Collectors.toSet());
    }
}
