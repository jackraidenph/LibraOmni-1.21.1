package dev.jackraidenph.libraomni.reflect;

import com.google.common.collect.Streams;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.ModMetadata;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.math.graph.HashDirectedGraph;
import dev.jackraidenph.libraomni.math.graph.IndexedGraph;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class RuntimeTaskProcessor implements LifecycleSetup {

    private final Map<Class<? extends RuntimeTask>, RuntimeTask> nativeTasks = new HashMap<>();

    private final ModContextManager modContextManager;
    private final ModMetadataReader modMetadataReader;

    private boolean setup = false;

    public RuntimeTaskProcessor(ModMetadataReader reader, ModContextManager modContextManager) {
        this.modContextManager = modContextManager;
        this.modMetadataReader = reader;
    }

    public RuntimeTaskProcessor registerTask(RuntimeTask task) {
        if (setup) {
            throw new AlreadySetupException();
        }

        Class<? extends RuntimeTask> clazz = task.getClass();
        if (nativeTasks.containsKey(clazz)) {
            throw new DuplicateTaskException(task);
        }

        nativeTasks.put(clazz, task);

        return this;
    }

    @Override
    public void subscribeAll(IEventBus eventBus) {
        if (this.setup) {
            throw new AlreadySetupException();
        }
        LifecycleSetup.super.subscribeAll(eventBus);
        this.setup = true;
    }

    @Override
    public void setupConstruct(FMLConstructModEvent event) {
        event.enqueueWork(() -> this.processAllModsForScope(LifecycleStage.CONSTRUCT));
    }

    @Override
    public void setupCommon(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> this.processAllModsForScope(LifecycleStage.COMMON));
    }

    @Override
    public void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> this.processAllModsForScope(LifecycleStage.CLIENT));
    }

    private Map<Class<? extends RuntimeTask>, RuntimeTask> getModTasks(String modId) {
        Map<Class<? extends RuntimeTask>, RuntimeTask> tasks = new HashMap<>();
        ModMetadata modMetadata = modMetadataReader.getModMetadata(modId);
        for (String taskName : modMetadata.getTasks()) {
            Class<? extends RuntimeTask> clazz = SafeReflectionUtil.forNameSubclass(taskName, RuntimeTask.class);
            if (clazz == null) {
                LibraOmni.LOGGER.error("Failed to get task class for name [{}]", taskName);
                continue;
            }
            RuntimeTask runtimeTask = SafeReflectionUtil.tryConstruct(clazz);
            if (runtimeTask == null) {
                LibraOmni.LOGGER.error("Failed to get construct task for name [{}]", taskName);
                continue;
            }

            if (tasks.containsKey(clazz)) {
                throw new DuplicateTaskException(runtimeTask);
            }

            tasks.put(clazz, runtimeTask);
        }

        return tasks;
    }

    private Set<TransitiveAnnotatedElement> elementsAnnotatedWith(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }
        return this.getTransitiveAnnotatedElements(modId).stream()
                .filter(e -> anyAnnotationPresent(e, annotations))
                .collect(Collectors.toSet());
    }

    private Set<TransitiveAnnotatedElement> getTransitiveAnnotatedElements(String modId) {
        return getElements(modId).stream().map(TransitiveAnnotatedElement::new).collect(Collectors.toSet());
    }

    private Set<AnnotatedElement> getElements(String modId) {
        return modMetadataReader.getModMetadata(modId).getAnnotatedData().getElements();
    }

    private static boolean anyAnnotationPresent(AnnotatedElement e, Set<Class<? extends Annotation>> annotations) {
        return annotations.stream().anyMatch(e::isAnnotationPresent);
    }

    private IndexedGraph<RuntimeTask> buildTaskGraph(Map<Class<? extends RuntimeTask>, RuntimeTask> tasksTable) {
        IndexedGraph<RuntimeTask> taskGraph = new HashDirectedGraph<>();

        if (tasksTable == null || tasksTable.isEmpty()) {
            return taskGraph;
        }

        taskGraph.addNode(0, null);

        int idx = 0;
        for (RuntimeTask task : tasksTable.values()) {
            taskGraph.addNode(++idx, task);
            taskGraph.addEdge(0, idx);
            for (Class<? extends RuntimeTask> requiredType : task.dependsOn()) {
                RuntimeTask requiredTask = tasksTable.get(requiredType);
                if (requiredTask == null) {
                    LibraOmni.LOGGER.error("Failed to fetch required task of type {}", requiredType);
                    throw new IllegalStateException();
                }

                if (taskGraph.getNodeIndex(requiredTask) < 0) {
                    taskGraph.addNode(++idx, requiredTask);
                } else {
                    taskGraph.removeEdge(0, requiredTask);
                }

                taskGraph.addEdge(task, requiredTask);
                if (taskGraph.hasCycles()) {
                    throw new IllegalStateException("Cyclic dependency %s -[depends]-> %s".formatted(
                            task.getClass().getSimpleName(),
                            requiredTask.getClass().getSimpleName()
                    ));
                }
            }
        }


        return taskGraph;
    }

    public void processAllModsForScope(LifecycleStage scope) {
        for (ModContext modContext : modContextManager.contexts()) {
            Map<Class<? extends RuntimeTask>, RuntimeTask> tasksForMod = Streams.concat(
                            nativeTasks.entrySet().stream(),
                            getModTasks(modContext.modId()).entrySet().stream()
                    )
                    .filter(t -> t.getValue().getExecutionStage().equals(scope))
                    .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

            IndexedGraph<RuntimeTask> taskGraph = buildTaskGraph(tasksForMod);
            if (taskGraph.getNodes().isEmpty()) {
                return;
            }

            LibraOmni.LOGGER.info("({}) Task graph created for mod [{}]", scope, modContext.modId());

            Stack<RuntimeTask> executionStack = new Stack<>();
            Iterator<RuntimeTask> bfi = taskGraph.breadthFirstIterator();
            while (bfi.hasNext()) {
                RuntimeTask task = bfi.next();
                if (task != null) {
                    executionStack.push(task);
                }
            }

            while (!executionStack.empty()) {
                RuntimeTask runtimeTask = executionStack.pop();
                if (runtimeTask == null) {
                    continue;
                }

                Set<TransitiveAnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeTask.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info("({}) Invoking {} for [{}]", scope, runtimeTask.getClass().getSimpleName(), modContext.modId());

                runtimeTask.process(modContext, elements);
            }
        }
    }

    public static class DuplicateTaskException extends IllegalArgumentException {
        private final RuntimeTask duplicate;

        public DuplicateTaskException(RuntimeTask task) {
            this.duplicate = task;
        }

        @Override
        public String getMessage() {
            return duplicate.getClass().getSimpleName();
        }
    }

    public static class AlreadySetupException extends IllegalStateException {
        @Override
        public String getMessage() {
            return "Setup was already performed";
        }
    }
}
