package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;
import dev.jackraidenph.libraomni.reflect.context.ModContext;
import dev.jackraidenph.libraomni.reflect.context.ModContextManager;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public enum RuntimeTaskProcessor {

    INSTANCE;

    private final Map<String, ElementData> elementDataMap = new HashMap<>();
    private final Map<Scope, List<RuntimeTask>> processors = new HashMap<>();
    private final Set<ModContext> modsToProcess = new HashSet<>();

    private boolean setup = false;

    private void initContextRegisters() {
        ModContextManager.INSTANCE.contexts().forEach(ModContext::initRegisters);
    }

    public void setup(IEventBus libraOmniEventBus) {
        if (this.setup) {
            LibraOmni.LOGGER.error("RuntimeProcessorManager already initialized!");
            return;
        }

        this.registerAnnotatedProcessors();

        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueConstruct);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueCommon);
        libraOmniEventBus.addListener(EventPriority.HIGHEST, this::enqueueClient);

        this.setup = true;
    }

    private void registerMods() {
        MetadataFileReader reader = MetadataFileReader.INSTANCE;
        ModContextManager contextManager = ModContextManager.INSTANCE;

        Set<Metadata> modsData = reader.findModsWithElementData();
        for (Metadata metadata : modsData) {
            String modId = metadata.getModId();
            ModContext context = contextManager.existsForMod(modId)
                    ? contextManager.getContext(modId)
                    : contextManager.createContext(modId);
            this.registerMod(context);
        }
    }

    private void registerAnnotatedProcessors() {
        MetadataFileReader reader = MetadataFileReader.INSTANCE;
        for (Metadata metadata : reader.readAllModData()) {
            for (Scope scope : Scope.values()) {
                for (String runtimeProcessorClass : metadata.getRuntimeProcessors(scope)) {
                    try {
                        Class<? extends RuntimeTask> clazz = Class.forName(runtimeProcessorClass).asSubclass(RuntimeTask.class);
                        Constructor<? extends RuntimeTask> constructor = clazz.getDeclaredConstructor();
                        RuntimeTask runtimeTask = constructor.newInstance();
                        this.registerProcessor(scope, runtimeTask);
                    } catch (ClassNotFoundException classNotFoundException) {
                        LibraOmni.LOGGER.error("Failed to instantiate {}, the class does not exist!", runtimeProcessorClass);
                    } catch (ClassCastException classCastException) {
                        throw new IllegalArgumentException(runtimeProcessorClass + " does not implement RuntimeProcessor");
                    } catch (NoSuchMethodException noConstructor) {
                        throw new IllegalStateException("No empty constructor found for " + runtimeProcessorClass);
                    } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
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

    private Set<RuntimeTask> allProcessors() {
        return this.processors.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public void registerProcessor(Scope scope, RuntimeTask runTimeTask) {
        if (this.allProcessors().contains(runTimeTask)) {
            throw new IllegalArgumentException("Runtime processor already registered");
        }
        this.processors.computeIfAbsent(scope, s -> new ArrayList<>()).add(runTimeTask);
    }

    public void processAll(Scope scope) {
        List<RuntimeTask> processors = this.processors.get(scope);
        if (processors == null || processors.isEmpty()) {
            return;
        }

        for (ModContext modContext : this.modsToProcess) {
            for (RuntimeTask runtimeTask : processors) {
                Set<AnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeTask.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info(
                        "({}) Invoking {} for {}",
                        scope,
                        runtimeTask.getClass().getSimpleName(),
                        modContext.modId()
                );

                runtimeTask.process(modContext, elements);
            }
        }
    }
}
