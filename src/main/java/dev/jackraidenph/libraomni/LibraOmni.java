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

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        ModMetadataReader modMetadataReader = new ModMetadataReader();
        modMetadataReader.readMetadataFile();

        ModContextManager modContextManager = new ModContextManager(modMetadataReader);
        modContextManager.subscribeAll(modEventBus);

        RuntimeTaskProcessor runtimeTaskProcessor = new RuntimeTaskProcessor(modMetadataReader, modContextManager);
        runtimeTaskProcessor.registerTask(new RegisterObjectsTask());
        runtimeTaskProcessor.subscribeAll(modEventBus);
    }
}
