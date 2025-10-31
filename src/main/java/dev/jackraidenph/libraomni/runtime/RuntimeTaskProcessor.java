package dev.jackraidenph.libraomni.runtime;

import com.google.common.collect.Streams;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.data.proxy.ProxyAnnotatedElement;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.exception.AlreadyInitializedException;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.ModMetadata;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.exception.DuplicateTaskException;
import dev.jackraidenph.libraomni.math.graph.HashDirectedGraph;
import dev.jackraidenph.libraomni.math.graph.IndexedGraph;
import dev.jackraidenph.libraomni.runtime.task.RuntimeTask;
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

    void registerTask(RuntimeTask task) {
        if (setup) {
            throw new AlreadyInitializedException();
        }

        Class<? extends RuntimeTask> clazz = task.getClass();
        if (nativeTasks.containsKey(clazz)) {
            throw new DuplicateTaskException(task);
        }

        nativeTasks.put(clazz, task);
    }

    @Override
    public void listenToBus(IEventBus eventBus) {
        if (this.setup) {
            throw new AlreadyInitializedException();
        }
        eventBus.addListener(this::setupConstruct);
        eventBus.addListener(this::setupCommon);
        eventBus.addListener(this::setupClient);
        this.setup = true;
    }

    @Override
    public void setupConstruct(FMLConstructModEvent event) {
        event.enqueueWork(() -> this.processLifecycleStage(LifecycleStage.CONSTRUCT));
    }

    @Override
    public void setupCommon(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> this.processLifecycleStage(LifecycleStage.COMMON));
    }

    @Override
    public void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> this.processLifecycleStage(LifecycleStage.CLIENT));
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
            RuntimeTask runtimeTask = UnsafeReflectionUtil.tryConstruct(clazz);

            if (tasks.containsKey(clazz)) {
                throw new DuplicateTaskException(runtimeTask);
            }

            tasks.put(clazz, runtimeTask);
        }

        return tasks;
    }

    private Set<ProxyAnnotatedElement> elementsAnnotatedWith(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }
        return this.getAnnotationAccessors(modId).stream()
                .filter(e -> anyAnnotationPresent(e, annotations))
                .collect(Collectors.toSet());
    }

    private Set<ProxyAnnotatedElement> getAnnotationAccessors(String modId) {
        return getElements(modId).stream()
                .map(ProxyFactory::proxifyAnnotatedElement)
                .collect(Collectors.toSet());
    }

    private Set<AnnotatedElement> getElements(String modId) {
        return modMetadataReader.getModMetadata(modId).getAnnotatedData().getElements();
    }

    private static boolean anyAnnotationPresent(ProxyAnnotatedElement e, Set<Class<? extends Annotation>> annotations) {
        return annotations.stream().anyMatch(e::isAnnotationPresent);
    }

    private IndexedGraph<RuntimeTask> buildTaskGraph(Map<Class<? extends RuntimeTask>, RuntimeTask> tasksTable) {
        IndexedGraph<RuntimeTask> taskGraph = new HashDirectedGraph<>();

        if (tasksTable == null || tasksTable.isEmpty()) {
            return taskGraph;
        }

        taskGraph.addNode(0, null);

        int i = 1;
        for (RuntimeTask task : tasksTable.values()) {
            taskGraph.addNode(i, task);
            taskGraph.addEdge(0, i);
            i++;
        }

        for (RuntimeTask task : tasksTable.values()) {
            int taskIndex = taskGraph.getNodeIndex(task);
            for (Class<? extends RuntimeTask> requiredType : task.dependsOn()) {
                RuntimeTask requiredTask = tasksTable.get(requiredType);
                if (requiredTask == null) {
                    throw new IllegalStateException("Failed to fetch required task of type [%s], check if tasks exist at the same lifecycle stage".formatted(requiredType));
                }
                int requiredIndex = taskGraph.getNodeIndex(requiredTask);
                taskGraph.addEdge(taskIndex, requiredIndex);
                if (taskGraph.hasCycles()) {
                    throw new IllegalStateException("Task graph forms a cycle, loop-closing pair: [%s]->[%s]".formatted(task.getClass().getSimpleName(), requiredTask.getClass().getSimpleName()));
                }
                taskGraph.removeEdge(0, requiredIndex);
            }
        }

        return taskGraph;
    }

    private void processLifecycleStage(LifecycleStage stage) {
        for (ModContext modContext : modContextManager.contexts()) {
            Map<Class<? extends RuntimeTask>, RuntimeTask> tasksForMod = Streams.concat(
                            nativeTasks.entrySet().stream(),
                            getModTasks(modContext.modId()).entrySet().stream()
                    )
                    .filter(t -> t.getValue().getExecutionStage().equals(stage))
                    .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

            IndexedGraph<RuntimeTask> taskGraph = buildTaskGraph(tasksForMod);
            if (taskGraph.getNodes().isEmpty()) {
                return;
            }

            LibraOmni.LOGGER.info("({}) Task graph created for mod [{}]", stage, modContext.modId());

            Deque<RuntimeTask> executionStack = new ArrayDeque<>();
            taskGraph.breadthFirstIterator().forEachRemaining(task -> Optional.ofNullable(task).ifPresent(executionStack::push));
            LibraOmni.LOGGER.info("({}) Formed execution stack {}", stage, executionStack);

            while (!executionStack.isEmpty()) {
                RuntimeTask runtimeTask = executionStack.pop();

                Set<ProxyAnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeTask.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info("({}) Invoking {} for [{}]", stage, runtimeTask.getClass().getSimpleName(), modContext.modId());

                runtimeTask.process(elements, modContext);
            }
        }
    }

}
