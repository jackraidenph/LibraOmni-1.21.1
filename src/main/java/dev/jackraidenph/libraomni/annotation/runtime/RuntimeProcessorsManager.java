package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.context.ModContextManager;
import dev.jackraidenph.libraomni.util.data.ElementData;
import dev.jackraidenph.libraomni.util.data.Metadata;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.util.context.ModContext;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager.Reader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeProcessorsManager {

    private final Map<String, ElementData> elementDataMap = new HashMap<>();
    private final Map<Scope, List<RuntimeProcessor>> processors = new HashMap<>();
    private final Set<ModContext> modsToProcess = new HashSet<>();

    private boolean setup = false;

    private static RuntimeProcessorsManager INSTANCE;

    public static RuntimeProcessorsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuntimeProcessorsManager();
        }

        return INSTANCE;
    }

    private void createMissingContexts() {
        ModList modList = ModList.get();
        for (Metadata modData : MetadataFileManager.getReader().readAllModData()) {
            String id = modData.getModId();
            if (!modList.isLoaded(id)) {
                continue;
            }

            ModContextManager.getInstance().createContext(id);
        }
    }

    private void initContextRegisters() {
        ModContextManager.getInstance().contexts().forEach(ModContext::initRegisters);
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
        Reader reader = MetadataFileManager.getReader();
        ModContextManager contextManager = ModContextManager.getInstance();

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
        Reader reader = MetadataFileManager.getReader();
        for (Metadata metadata : reader.readAllModData()) {
            for (Scope scope : Scope.values()) {
                for (String runtimeProcessorClass : metadata.getRuntimeProcessors(scope)) {
                    try {
                        //Potential exception is handled
                        //noinspection unchecked
                        Class<? extends RuntimeProcessor> clazz = (Class<? extends RuntimeProcessor>) Class.forName(runtimeProcessorClass);
                        Constructor<? extends RuntimeProcessor> constructor = clazz.getDeclaredConstructor();
                        RuntimeProcessor runtimeProcessor = constructor.newInstance();
                        this.registerProcessor(scope, runtimeProcessor);
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

    private RuntimeProcessorsManager() {
    }

    private Set<AnnotatedElement> readElements(String modId) {
        if (elementDataMap.containsKey(modId)) {
            return elementDataMap.get(modId).getElements();
        }

        ElementData elementData = MetadataFileManager.getReader().readElementData(modId);
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

    private Set<RuntimeProcessor> allProcessors() {
        return this.processors.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public void registerProcessor(Scope scope, RuntimeProcessor runTimeProcessor) {
        if (this.allProcessors().contains(runTimeProcessor)) {
            throw new IllegalArgumentException("Runtime processor already registered");
        }
        this.processors.computeIfAbsent(scope, s -> new ArrayList<>()).add(runTimeProcessor);
    }

    public void processAll(Scope scope) {
        List<RuntimeProcessor> processors = this.processors.get(scope);
        if (processors == null || processors.isEmpty()) {
            return;
        }

        for (ModContext modContext : this.modsToProcess) {
            for (RuntimeProcessor runtimeProcessor : processors) {
                Set<AnnotatedElement> elements = this.elementsAnnotatedWith(
                        modContext.modId(),
                        runtimeProcessor.getSupportedAnnotations()
                );

                LibraOmni.LOGGER.info(
                        "Processing mod id [{}] with [{}] in {}",
                        modContext.modId(),
                        runtimeProcessor.getClass().getSimpleName(),
                        scope
                );

                runtimeProcessor.process(modContext, elements);
            }
        }
    }
}
