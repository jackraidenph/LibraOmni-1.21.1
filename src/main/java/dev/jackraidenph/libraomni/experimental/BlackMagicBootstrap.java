package dev.jackraidenph.libraomni.experimental;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DeferredWorkQueue;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModWorkManager;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.ModLifecycleEvent;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BlackMagicBootstrap {

    private static boolean BLACK_MAGIC_MODE = false;

    public static boolean isBlackMagicActive() {
        return BLACK_MAGIC_MODE;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void bootstrapBlackMagic(ModIdGetter modLocator, ProcessingContext processingContext) {
        BLACK_MAGIC_MODE = true;

        Map<String, Class<?>> classes = BlackMagicUtil.compileAndLoad(processingContext);

        SharedConstants.tryDetectVersion();

        List<String> mods = new ArrayList<>(modLocator.discoveredMods());
        List<ModContainer> fakeContainers = new ArrayList<>();

        for (String modId : mods) {
            List<Class<?>> modClasses = new ArrayList<>();
            modLocator.getModClasses(modId).stream()
                    .map(classes::get)
                    .filter(Objects::nonNull)
                    .forEach(modClasses::add);
            fakeContainers.add(new FakeModContainer(modId, modClasses));
        }

        ModContainer libraOmniContainer = new FakeModContainer(LibraOmni.MOD_ID, List.of(LibraOmni.class));
        fakeContainers.addFirst(libraOmniContainer);

        List<ModInfo> modInfos = fakeContainers.stream()
                .map(ModContainer::getModInfo)
                .map(ModInfo.class::cast)
                .collect(Collectors.toList());

        ModList.of(List.of(), modInfos);
        try {
            Method invokeSetMods = ModList.class.getDeclaredMethod("setLoadedMods", List.class);
            invokeSetMods.setAccessible(true);
            invokeSetMods.invoke(ModList.get(), fakeContainers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LoadingModList.of(List.of(), List.of(), modInfos, List.of(), Map.of());

        Bootstrap.bootStrap();

        IEventBus libraOmniEventBus = libraOmniContainer.getEventBus();
        if (libraOmniEventBus != null) {
            registerNeoForgeRegistry("ATTRIBUTES", libraOmniEventBus);
            registerNeoForgeRegistry("COMMAND_ARGUMENT_TYPES", libraOmniEventBus);
            registerNeoForgeRegistry("BIOME_MODIFIER_SERIALIZERS", libraOmniEventBus);
            registerNeoForgeRegistry("STRUCTURE_MODIFIER_SERIALIZERS", libraOmniEventBus);
            registerNeoForgeRegistry("HOLDER_SET_TYPES", libraOmniEventBus);
            registerNeoForgeRegistry("VANILLA_FLUID_TYPES", libraOmniEventBus);
            registerNeoForgeRegistry("ENTITY_PREDICATE_CODECS", libraOmniEventBus);
            registerNeoForgeRegistry("ITEM_SUB_PREDICATES", libraOmniEventBus);
            registerNeoForgeRegistry("INGREDIENT_TYPES", libraOmniEventBus);
            registerNeoForgeRegistry("CONDITION_CODECS", libraOmniEventBus);
            registerNeoForgeRegistry("GLOBAL_LOOT_MODIFIER_SERIALIZERS", libraOmniEventBus);

            NeoForgeRegistriesSetup.setup(libraOmniEventBus);
            ModifyRegistriesEvent modifyRegistriesEvent = UnsafeReflectionUtil.tryConstruct(ModifyRegistriesEvent.class);
            NewRegistryEvent newRegistryEvent = UnsafeReflectionUtil.tryConstruct(NewRegistryEvent.class);
            libraOmniEventBus.post(modifyRegistriesEvent);
            libraOmniEventBus.post(newRegistryEvent);
        }

        ProcessingEnvironment processingEnvironment = processingContext.processingEnvironment();
        Logger messagerLogger = new MessagerLogger(LibraOmni.class, processingEnvironment.getMessager());
        bootstrapLibraOmni(messagerLogger, processingEnvironment.getFiler());

        LibraOmni.LOGGER.warn("Launching LibraOmni in compilation processing mode, best of luck");

        fakeContainers.stream()
                .map(FakeModContainer.class::cast)
                .forEach(FakeModContainer::construct);

        for (ModContainer container : fakeContainers) {
            executeSyncLifeCycleEvent("Contruct", container, FMLConstructModEvent::new);
        }

        GameData.unfreezeData();

        for (ResourceLocation rootRegistryName : GameData.getRegistrationOrder()) {
            ResourceKey<? extends Registry<?>> registryKey = ResourceKey.createRegistryKey(rootRegistryName);
            Registry<?> registry = Objects.requireNonNull(BuiltInRegistries.REGISTRY.get(rootRegistryName));

            try {
                Constructor<RegisterEvent> constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
                constructor.setAccessible(true);
                RegisterEvent registerEvent = constructor.newInstance(registryKey, registry);
                for (EventPriority phase : EventPriority.values()) {
                    ModList.get().forEachModInOrder(mc -> mc.acceptEvent(phase, registerEvent));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        GameData.freezeData();

        for (ModContainer container : fakeContainers) {
            executeSyncLifeCycleEvent("Common", container, FMLCommonSetupEvent::new);
            executeSyncLifeCycleEvent("Client", container, FMLClientSetupEvent::new);
        }
    }

    private static void executeSyncLifeCycleEvent(
            String name,
            ModContainer modContainer,
            BiFunction<ModContainer, DeferredWorkQueue, ModLifecycleEvent> eventBiFunction
    ) {
        Executor syncExecutor = ModWorkManager.syncExecutor();
        DeferredWorkQueue workQueue = new DeferredWorkQueue("Libra Omni Synthetic Compilation Event [%s]".formatted(name));
        ModLifecycleEvent event = eventBiFunction.apply(modContainer, workQueue);
        modContainer.acceptEvent(event);

        try {
            CompletableFuture.runAsync(workQueue::runTasks, syncExecutor).get(50, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failure during executing life cycle event [%s]: ".formatted(name), e);
        }
    }

    private static void bootstrapLibraOmni(Logger logger, Filer filer) {
        try {
            LoggerWrapper wrapper = (LoggerWrapper) LibraOmni.LOGGER;
            wrapper.setLogger(logger);

            Field filerField = ModMetadataReader.class.getDeclaredField("COMPILATION_FILER");
            filerField.setAccessible(true);
            filerField.set(null, filer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void registerNeoForgeRegistry(String name, IEventBus eventBus) {
        try {
            Field field = NeoForgeMod.class.getDeclaredField(name);
            field.setAccessible(true);
            DeferredRegister<?> register = (DeferredRegister<?>) field.get(null);
            register.register(eventBus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
