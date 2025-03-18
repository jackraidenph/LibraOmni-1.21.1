package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.annotation.compile.util.MetadataFileManager;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.Metadata.ModData;
import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisteredAnnotationProcessor;
import dev.jackraidenph.libraomni.annotation.run.util.ModContext;
import dev.jackraidenph.libraomni.annotation.run.util.ModContextManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import org.slf4j.Logger;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    //TODO ADD ACTUAL METADATA CREATION
    //TODO REARRANGE PACKAGES
    //TODO MOVE SHIT BELOW INTO RUNTIME PROCESSOR INITIALIZATION
    //TODO ADD HIERARCHICAL CLASS LOOKUP FOR REGISTRIES
    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeProcessorsManager runtimeProcessorsManager = RuntimeProcessorsManager.getInstance();

        runtimeProcessorsManager.registerProcessor(Scope.CONSTRUCT, new RegisteredAnnotationProcessor());

        modEventBus.addListener((FMLConstructModEvent event) -> event.enqueueWork(
                () -> {
                    this.createMissingContexts();
                    this.initContextRegisters();

                    this.registerMods();
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

    private void createMissingContexts() {
        ModList modList = ModList.get();
        for (ModData modData : MetadataFileManager.reader().readAllModData()) {
            String id = modData.modId();
            if (!modList.isLoaded(id)) {
                continue;
            }

            modList.getModContainerById(id).ifPresent(container -> {
                ModContextManager contextManager = ModContextManager.get();
                if (!contextManager.existsForMod(id)) {
                    contextManager.newContext(container);
                }
            });
        }
    }

    private void initContextRegisters() {
        ModContextManager.get().contexts().forEach(ModContext::initRegisters);
    }

    private void registerMods() {
        ModContextManager.get().contexts().forEach(RuntimeProcessorsManager.getInstance()::registerMod);
    }

    public static ClassLoader classLoader() {
        return LibraOmni.class.getClassLoader();
    }
}
