package dev.jackraidenph.libraomni.reflect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;
import dev.jackraidenph.libraomni.common.data.ElementData;
import dev.jackraidenph.libraomni.common.data.Metadata;
import dev.jackraidenph.libraomni.common.data.MetadataFileReader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeTaskProcessor {

    private final Map<String, ElementData> elementDataMap = new HashMap<>();
    private final Multimap<Scope, RuntimeTask> taskHolder = ArrayListMultimap.create();
    private final Set<ModContext> modsToProcess = new HashSet<>();

    private final ModContextManager modContextManager;

    private boolean setup = false;

    private RuntimeTaskProcessor(ModContextManager modContextManager) {
        this.modContextManager = modContextManager;
    }

    private void initContextRegisters() {
        modContextManager.contexts().forEach(ModContext::initRegisters);
    }

    public void registerTask(Scope scope, RuntimeTask task) {
        if (setup) {
            throw new IllegalStateException("Processor was already set up");
        }

        Collection<RuntimeTask> tasksForScope = taskHolder.get(scope);

        if (!tasksForScope.contains(task)) {
            tasksForScope.add(task);
        }
    }

    public void setup(IEventBus libraOmniEventBus) {
        if (this.setup) {
            LibraOmni.LOGGER.error("RuntimeProcessorManager already initialized!");
            return;
        }

        this.registerAnnotatedTasks();

        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueConstruct);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueCommon);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueClient);

        this.setup = true;
    }

    private void registerMods() {
        MetadataFileReader reader = MetadataFileReader.INSTANCE;

        Set<Metadata> modsData = reader.findModsWithElementData();
        for (Metadata metadata : modsData) {
            String modId = metadata.getModId();
            ModContext context = modContextManager.existsForMod(modId)
                    ? modContextManager.getContext(modId)
                    : modContextManager.createContext(modId);
            this.registerMod(context);
        }
    }

    private void registerAnnotatedTasks() {
        MetadataFileReader.INSTANCE.readAllModData().forEach(this::processMetadata);
    }

    private void processMetadata(Metadata metadata) {
        for (Scope scope : Scope.values()) {
            tryRegisterTasksByAnnotation(metadata, scope);
        }
    }

    private void tryRegisterTasksByAnnotation(Metadata metadata, Scope scope) {
        for (String taskName : metadata.runtimeTasksForScope(scope)) {
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
                    this.registerMods();
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

    public boolean isSetup() {
        return this.setup;
    }

    private Set<AnnotatedElement> readElements(String modId) {
        if (elementDataMap.containsKey(modId)) {
            return elementDataMap.get(modId).getElements();
        }

        ElementData elementData = MetadataFileReader.INSTANCE.readElementData(modId);
        if (elementData != null) {
            this.elementDataMap.put(modId, elementData);
            return elementData.getElements();
        }

        return Set.of();
    }

    private static boolean anyAnnotationPresent(AnnotatedElement e, Set<Class<? extends Annotation>> annotations) {
        for (Class<? extends Annotation> a : annotations) {
            if (e.isAnnotationPresent(a)) {
                return true;
            }
        }

        return false;
    }

    private Set<AnnotatedElement> elementsAnnotatedWith(String modId, Set<Class<? extends Annotation>> annotations) {
        if (annotations.isEmpty()) {
            return Set.of();
        }
        return this.readElements(modId).stream()
                .filter(e -> anyAnnotationPresent(e, annotations))
                .collect(Collectors.toSet());
    }

    public void registerMod(ModContext modContext) {
        this.modsToProcess.add(modContext);
    }

    public void processAll(Scope scope) {
        Collection<RuntimeTask> tasks = taskHolder.get(scope);
        if (tasks.isEmpty()) {
            return;
        }

        for (ModContext modContext : this.modsToProcess) {
            for (RuntimeTask runtimeTask : tasks) {
                Set<AnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeTask.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info("({}) Invoking {} for {}", scope, runtimeTask.getClass().getSimpleName(), modContext.modId());

                runtimeTask.process(modContext, elements);
            }
        }
    }

    public static RuntimeTaskProcessorConfigurator withContextManager(ModContextManager modContextManager) {
        return new RuntimeTaskProcessorConfigurator(new RuntimeTaskProcessor(modContextManager));
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
