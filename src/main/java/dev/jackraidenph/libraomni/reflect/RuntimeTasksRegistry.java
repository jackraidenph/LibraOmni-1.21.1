package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;

import java.util.*;
import java.util.function.Supplier;

public class RuntimeTasksRegistry {

    private static final Map<Scope, List<Supplier<RuntimeTask>>> PROCESSORS_REGISTRY = new HashMap<>();

    static {
        registerAll(Scope.CONSTRUCT,
                RegisterObjectTask::new
        );
    }

    private static void register(Scope scope, Supplier<RuntimeTask> processorSupplier) {
        PROCESSORS_REGISTRY.computeIfAbsent(scope, k -> new ArrayList<>()).add(processorSupplier);
    }

    @SafeVarargs
    private static void registerAll(Scope scope, Supplier<RuntimeTask>... processorSuppliers) {
        for (Supplier<RuntimeTask> supplier : processorSuppliers) {
            register(scope, supplier);
        }
    }

    public static void init() {
        RuntimeTaskProcessor manager = RuntimeTaskProcessor.INSTANCE;
        for (Scope scope : PROCESSORS_REGISTRY.keySet()) {
            for (Supplier<RuntimeTask> processorSupplier : PROCESSORS_REGISTRY.get(scope)) {
                manager.registerProcessor(scope, processorSupplier.get());
            }
        }
    }
}
