package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.reflect.RegisterObjectTask;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;
import dev.jackraidenph.libraomni.reflect.RuntimeTaskProcessor;
import dev.jackraidenph.libraomni.reflect.context.ModContextManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        ModContextManager modContextManager = new ModContextManager();
        RuntimeTaskProcessor runtimeTaskProcessor = new RuntimeTaskProcessor(modContextManager);

        runtimeTaskProcessor.registerTask(Scope.CONSTRUCT, new RegisterObjectTask());
        runtimeTaskProcessor.setup(modEventBus);
    }
}
