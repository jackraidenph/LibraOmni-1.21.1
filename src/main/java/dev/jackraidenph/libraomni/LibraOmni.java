package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.annotation.runtime.RegisteredAnnotationProcessor;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessorsManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeProcessorsManager runtimeProcessorsManager = RuntimeProcessorsManager.INSTANCE;
        runtimeProcessorsManager.registerProcessor(Scope.CONSTRUCT, new RegisteredAnnotationProcessor());
        runtimeProcessorsManager.setup(modEventBus);
    }
}
