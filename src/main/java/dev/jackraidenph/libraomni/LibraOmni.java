package dev.jackraidenph.libraomni;

import com.mojang.logging.LogUtils;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.runtime.RegisteredAnnotationProcessor;
import dev.jackraidenph.libraomni.util.context.ModContext;
import dev.jackraidenph.libraomni.util.context.ModContextManager;
import dev.jackraidenph.libraomni.util.data.Metadata;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager.Reader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mod(LibraOmni.MODID)
public class LibraOmni {

    public static final String MODID = "libraomni";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibraOmni(IEventBus modEventBus, ModContainer modContainer) {
        RuntimeProcessorsManager runtimeProcessorsManager = RuntimeProcessorsManager.getInstance();
        runtimeProcessorsManager.registerProcessor(Scope.CONSTRUCT, new RegisteredAnnotationProcessor());
        Set<ModContext> mods = this.gatherModsToProcess();
        runtimeProcessorsManager.setup(modEventBus, mods);
    }

    private Set<ModContext> gatherModsToProcess() {
        Reader reader = MetadataFileManager.reader();
        ModContextManager contextManager = ModContextManager.get();
        ModList modList = ModList.get();

        Set<Metadata> modsData = reader.findModsWithElementData();
        Set<ModContext> contexts = new HashSet<>();
        for (Metadata metadata : modsData) {
            String modId = metadata.getModId();
            if (contextManager.existsForMod(modId)) {
                contexts.add(contextManager.forMod(modId));
            } else {
                Optional<? extends ModContainer> container = modList.getModContainerById(modId);
                if (container.isEmpty()) {
                    continue;
                }
                ModContext defaultContext = contextManager.newContext(container.get());
                contexts.add(defaultContext);
            }
        }

        return contexts;
    }

    public static ClassLoader classLoader() {
        return LibraOmni.class.getClassLoader();
    }
}
