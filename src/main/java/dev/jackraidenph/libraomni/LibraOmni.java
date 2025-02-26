package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.annotation.compile.impl.resource.AnnotationMapCreationProcessor;
import dev.jackraidenph.libraomni.context.ModContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
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
        modEventBus.addListener(this::enqueueCommonModContextJobs);
        modEventBus.addListener(this::enqueueClientModContextJobs);
        modEventBus.addListener(this::enqueueConstructModContextJobs);
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

    @SubscribeEvent
    public void enqueueCommonModContextJobs(FMLCommonSetupEvent commonSetupEvent) {
        commonSetupEvent.enqueueWork(() -> {
            for (ModContext modContext : MOD_CONTEXT_MAP.values()) {
                modContext.invokeCommon();
            }
        });
    }

    @SubscribeEvent
    public void enqueueClientModContextJobs(FMLClientSetupEvent clientSetupEvent) {
        clientSetupEvent.enqueueWork(() -> {
            for (ModContext modContext : MOD_CONTEXT_MAP.values()) {
                modContext.invokeClient();
            }
        });
    }

    @SubscribeEvent
    public void enqueueConstructModContextJobs(FMLConstructModEvent constructModEvent) {
        constructModEvent.enqueueWork(() -> {
            for (String annotationMap : AnnotationMapCreationProcessor.allAnnotationMaps()) {
                String modId = AnnotationMapCreationProcessor.extractModNameFromMapFile(annotationMap);

                if (!MOD_CONTEXT_MAP.containsKey(modId)) {
                    ModList.get().getModContainerById(modId).ifPresent(LibraOmni::createContext);
                }
            }

            for (ModContext modContext : MOD_CONTEXT_MAP.values()) {
                modContext.invokeConstruct();
            }
        });
    }

    public static ClassLoader classLoader() {
        return LibraOmni.class.getClassLoader();
    }

}
