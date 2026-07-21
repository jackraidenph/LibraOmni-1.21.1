package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.util.ObjectOriginGetter;
import dev.jackraidenph.libraomni.compilation.task.CompilationTaskProcessor;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.experimental.ForwardingLoggerWrapper;
import dev.jackraidenph.libraomni.runtime.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibraOmni.MOD_ID)
public class LibraOmni {

    public static final String MOD_ID = "libraomni";
    public static final Logger LOGGER = ForwardingLoggerWrapper.make(LogUtils.getLogger());
    private static ModContextManager CONTEXT_MANAGER = null;
    private static ModMetadataReader READER = null;

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Reading metadata files");
        ModMetadataReader modMetadataReader = new ModMetadataReader();
        modMetadataReader.readMetadataFile();
        if (modMetadataReader.initialized()) {
            READER = modMetadataReader;
        }
        LOGGER.info("Read metadata files, mods: {}", modMetadataReader.getAllModsWithMetadata());

        LOGGER.info("Creating ModContextManager");
        ModContextManager modContextManager = new ModContextManager(modMetadataReader);
        modContextManager.createContextsFromMetadata();
        StaticInit.initContextExtensions(modContextManager);
        modContextManager.listenToBus(modEventBus);
        CONTEXT_MANAGER = modContextManager;
        LOGGER.info("ModContextManager created");

        LOGGER.info("Creating RuntimeTaskProcessor");
        RuntimeTaskProcessor runtimeTaskProcessor = new RuntimeTaskProcessor(modMetadataReader, modContextManager);
        StaticInit.initRuntimeTasks(runtimeTaskProcessor);
        LOGGER.info("Registered runtime tasks");
        runtimeTaskProcessor.listenToBus(modEventBus);
        LOGGER.info("RuntimeTaskProcessor created");
    }

    public static ModContextManager getModContextManager() {
        return CONTEXT_MANAGER;
    }

    public static ObjectOriginGetter getModMetadataReader() {
        return READER;
    }

    public static ObjectOriginGetter getCurrentOriginGetter() {
        if (READER == null) {
            return CompilationTaskProcessor.getModIdGetter();
        } else {
            return getModMetadataReader();
        }
    }
}
