package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.reflect.ModContextManager;
import dev.jackraidenph.libraomni.reflect.RegisterObjectsTask;
import dev.jackraidenph.libraomni.reflect.RuntimeTaskProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibraOmni.MOD_ID)
public class LibraOmni {

    public static final String MOD_ID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static ModContextManager CONTEXT_MANAGER = null;

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Reading metadata files");
        ModMetadataReader modMetadataReader = new ModMetadataReader();
        modMetadataReader.readMetadataFile();
        LOGGER.info("Read metadata files, mods: {}", modMetadataReader.getAllModsWithMetadata());

        LOGGER.info("Creating ModContextManager");
        ModContextManager modContextManager = new ModContextManager(modMetadataReader);
        modContextManager.createContextsFromMetadata();
        modContextManager.subscribeAll(modEventBus);

        CONTEXT_MANAGER = modContextManager;
        LOGGER.info("ModContextManager created");

        LOGGER.info("Creating RuntimeTaskProcessor");
        RuntimeTaskProcessor runtimeTaskProcessor = new RuntimeTaskProcessor(modMetadataReader, modContextManager);
        runtimeTaskProcessor.registerTask(new RegisterObjectsTask());
        LOGGER.info("Registered runtime tasks");
        runtimeTaskProcessor.subscribeAll(modEventBus);
        LOGGER.info("RuntimeTaskProcessor created");
    }

    public static ModContextManager getModContextManager() {
        return CONTEXT_MANAGER;
    }
}
