package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;

import java.util.*;
import java.util.function.Supplier;

public class RuntimeProcessorRegistry {

    private static final Map<Scope, List<Supplier<RuntimeProcessor>>> PROCESSORS_REGISTRY = new HashMap<>();

    static {
        registerAll(Scope.CONSTRUCT,
                RegisteredAnnotationProcessor::new
        );
    }

    private static void register(Scope scope, Supplier<RuntimeProcessor> processorSupplier) {
        PROCESSORS_REGISTRY.computeIfAbsent(scope, k -> new ArrayList<>()).add(processorSupplier);
    }

    @SafeVarargs
    private static void registerAll(Scope scope, Supplier<RuntimeProcessor>... processorSuppliers) {
        for (Supplier<RuntimeProcessor> supplier : processorSuppliers) {
            register(scope, supplier);
        }
    }

    public static void init() {
        RuntimeProcessorsManager manager = RuntimeProcessorsManager.INSTANCE;
        for (Scope scope : PROCESSORS_REGISTRY.keySet()) {
            for (Supplier<RuntimeProcessor> processorSupplier : PROCESSORS_REGISTRY.get(scope)) {
                manager.registerProcessor(scope, processorSupplier.get());
            }
        }
    }
}
