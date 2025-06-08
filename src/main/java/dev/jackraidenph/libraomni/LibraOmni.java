package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.reflect.RuntimeTasksRegistry;
import dev.jackraidenph.libraomni.reflect.RuntimeTaskProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeTaskProcessor runtimeTaskProcessor = RuntimeTaskProcessor.INSTANCE;
        RuntimeTasksRegistry.init();
        runtimeTaskProcessor.setup(modEventBus);
    }
}
