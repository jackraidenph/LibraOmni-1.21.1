package dev.jackraidenph.libraomni.reflect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.data.ModMetadata;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RuntimeTaskProcessor {

    private final Multimap<Scope, RuntimeTask> tasksForScope = ArrayListMultimap.create();
    private final Set<ModContext> modsToProcess = new HashSet<>();

    private final ModContextManager modContextManager;
    private final ModMetadataReader modMetadataReader;

    private boolean setup = false;

    private RuntimeTaskProcessor(ModContextManager modContextManager, ModMetadataReader reader) {
        this.modContextManager = modContextManager;
        this.modMetadataReader = reader;
    }

    private void initContextRegisters() {
        modContextManager.contexts().forEach(ModContext::initRegisters);
    }

    public void registerTask(Scope scope, RuntimeTask task) {
        if (setup) {
            throw new IllegalStateException("Already set up");
        }

        Collection<RuntimeTask> tasks = this.tasksForScope.get(scope);

        if (!tasks.contains(task)) {
            tasks.add(task);
        }
    }

    public void setup(IEventBus libraOmniEventBus) {
        if (this.setup) {
            throw new IllegalStateException("Already set up");
        }

        this.registerAnnotatedTasks();

        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueConstruct);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueCommon);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueClient);

        this.setup = true;
    }

    private void createContextsFromMetadata() {
        for (String mod : modMetadataReader.getAllModsWithMetadata()) {
            ModContext context = modContextManager.getOrCreate(mod);
            this.registerMod(context);
        }
    }

    private void registerAnnotatedTasks() {
        modMetadataReader.getAllModMetadata().values().forEach(this::processMetadata);
    }

    private void processMetadata(ModMetadata modMetadata) {
        for (Scope scope : Scope.values()) {
            tryRegisterTasksFromAnnotation(modMetadata, scope);
        }
    }

    private void tryRegisterTasksFromAnnotation(ModMetadata modMetadata, Scope scope) {
        for (String taskName : modMetadata.tasksForScope(scope)) {
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
            this.registerTask(scope, runtimeTask);
        }
    }

    private void enqueueConstruct(FMLConstructModEvent constructModEvent) {
        constructModEvent.enqueueWork(
                () -> {
                    this.createContextsFromMetadata();
                    this.initContextRegisters();
                    this.processAll(Scope.CONSTRUCT);
                });
    }

    private void enqueueCommon(FMLCommonSetupEvent commonSetupEvent) {
        commonSetupEvent.enqueueWork(() -> this.processAll(Scope.COMMON));
    }

    private void enqueueClient(FMLClientSetupEvent clientSetupEvent) {
        clientSetupEvent.enqueueWork(() -> this.processAll(Scope.CLIENT));
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

    private Set<TransitiveAnnotatedElement> elementsAnnotatedWith(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }
        return this.getTransitiveAnnotatedElements(modId).stream()
                .filter(e -> anyAnnotationPresent(e, annotations))
                .collect(Collectors.toSet());
    }

    public void registerMod(ModContext modContext) {
        this.modsToProcess.add(modContext);
    }

    public void processAll(Scope scope) {
        Collection<RuntimeTask> tasks = tasksForScope.get(scope);
        if (tasks.isEmpty()) {
            return;
        }

        for (ModContext modContext : this.modsToProcess) {
            for (RuntimeTask runtimeTask : tasks) {
                Set<TransitiveAnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeTask.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info("({}) Invoking {} for {}", scope, runtimeTask.getClass().getSimpleName(), modContext.modId());

                runtimeTask.process(modContext, elements);
            }
        }
    }

    public static RuntimeTaskProcessorConfigurator with(ModContextManager modContextManager, ModMetadataReader modMetadataReader) {
        return new RuntimeTaskProcessorConfigurator(new RuntimeTaskProcessor(modContextManager, modMetadataReader));
    }

    public static class RuntimeTaskProcessorConfigurator {

        private final RuntimeTaskProcessor taskProcessor;

        public RuntimeTaskProcessorConfigurator(RuntimeTaskProcessor taskProcessor) {
            this.taskProcessor = taskProcessor;
        }

        public RuntimeTaskProcessorConfigurator registerTask(Scope scope, RuntimeTask runtimeTask) {
            taskProcessor.registerTask(scope, runtimeTask);
            return this;
        }

        public RuntimeTaskProcessor setup(IEventBus eventBus) {
            taskProcessor.setup(eventBus);
            return taskProcessor;
        }
    }
}
