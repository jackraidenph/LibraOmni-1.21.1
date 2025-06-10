package dev.jackraidenph.libraomni.processor;

import javax.annotation.processing.Messager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompilationTaskRegistry {

    private static final Set<Supplier<CompilationTask>> PROCESSORS_REGISTRY = new HashSet<>();

    static {
        registerAll(
                CreateMetadataTask::new,
                ValidateAnnotationsTask::new
        );
    }

    @SafeVarargs
    static void registerAll(Supplier<CompilationTask>... suppliers) {
        for (Supplier<CompilationTask> s : suppliers) {
            register(s);
        }
    }

    static void register(Supplier<CompilationTask> processorSupplier) {
        PROCESSORS_REGISTRY.add(processorSupplier);
    }

    static Collection<CompilationTask> getAll(Messager messager) {
        return PROCESSORS_REGISTRY.stream()
                .map(Supplier::get)
                .peek(task -> messager.printNote("Registered " + task.getClass().getSimpleName() + " for processing"))
                .collect(Collectors.toSet());
    }
}
