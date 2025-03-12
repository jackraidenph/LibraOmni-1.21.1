package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.annotation.compile.impl.resource.AnnotationMapProcessor;
import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisteredAnnotationProcessor;
import dev.jackraidenph.libraomni.annotation.run.util.ModContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, ModContext> MOD_CONTEXT_MAP = new HashMap<>();

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeProcessorsManager runtimeProcessorsManager = RuntimeProcessorsManager.getInstance();

        runtimeProcessorsManager.registerProcessor(Scope.CONSTRUCT, new RegisteredAnnotationProcessor());

        modEventBus.addListener((FMLConstructModEvent event) -> event.enqueueWork(
                () -> {
                    this.createDefaultContexts();
                    this.initContextRegisters();

                    this.prepareForProcessing();
                    runtimeProcessorsManager.processAll(Scope.CONSTRUCT);
                })
        );

        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(
                () -> runtimeProcessorsManager.processAll(Scope.COMMON))
        );

        modEventBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(
                () -> runtimeProcessorsManager.processAll(Scope.CLIENT))
        );
    }

    public static ModContext createContext(ModContainer modContainer) {
        if (MOD_CONTEXT_MAP.containsKey(modContainer.getModId())) {
            throw new IllegalStateException("Context for " + modContainer + " was already opened");
        }
        ModContext modContext = new ModContext(modContainer);
        LOGGER.info("Opened context for {}", modContainer.getModId());
        MOD_CONTEXT_MAP.put(modContainer.getModId(), modContext);
        return modContext;
    }

    public static ModContext getContext(String modId) {
        if (!MOD_CONTEXT_MAP.containsKey(modId)) {
            throw new IllegalArgumentException("Context for " + modId + " was never opened");
        }
        return MOD_CONTEXT_MAP.get(modId);
    }

    private void createDefaultContexts() {
        for (String annotationMap : AnnotationMapProcessor.allAnnotationMaps()) {
            String modId = AnnotationMapProcessor.extractModNameFromMapFile(annotationMap);

            if (!MOD_CONTEXT_MAP.containsKey(modId)) {
                ModList.get().getModContainerById(modId).ifPresent(LibraOmni::createContext);
            }
        }
    }

    private void initContextRegisters() {
        for (ModContext modContext : MOD_CONTEXT_MAP.values()) {
            modContext.initRegisters();
        }
    }

    private void prepareForProcessing() {
        for (ModContext modContext : MOD_CONTEXT_MAP.values()) {
            RuntimeProcessorsManager.getInstance().prepareForMod(modContext);
        }
    }

    public static ClassLoader classLoader() {
        return LibraOmni.class.getClassLoader();
    }
}
