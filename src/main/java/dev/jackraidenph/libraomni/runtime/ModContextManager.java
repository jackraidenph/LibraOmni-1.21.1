package dev.jackraidenph.libraomni.runtime;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.data.ModMetadataReader;
import dev.jackraidenph.libraomni.runtime.extension.AbstractModContextExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.*;
import java.util.function.Function;

public class ModContextManager implements LifecycleSetup {

    private final Map<String, ModContext> contextMap = new HashMap<>();
    private final ModMetadataReader modMetadataReader;

    public ModContextManager(ModMetadataReader modMetadataReader) {
        this.modMetadataReader = modMetadataReader;
    }

    void registerExtensionFactory(Function<ModContext, AbstractModContextExtension> extension) {
        for (ModContext context : contexts()) {
            context.registerExtension(extension.apply(context));
        }
    }

    public void createContextsFromMetadata() {
        for (String mod : modMetadataReader.getAllModsWithMetadata()) {
            this.getOrCreate(mod);
        }
    }

    public ModContext getOrCreate(String modId) {
        return existsForMod(modId) ? getContext(modId) : createContext(modId);
    }

    private ModContext getContext(String modId) {
        if (!this.contextMap.containsKey(modId)) {
            throw new IllegalStateException("No context found for [" + modId + "]");
        }

        return this.contextMap.get(modId);
    }

    public ModContext createContext(String modId) {
        ModList modList = ModList.get();
        Optional<? extends ModContainer> modContainerOptional = modList.getModContainerById(modId);
        ModContainer modContainer = modContainerOptional.orElseThrow(() -> new IllegalArgumentException("No ModContainer exists for [" + modId + "]"));
        return createContext(modContainer);
    }

    private ModContext createContext(ModContainer modContainer) {
        ModContext modContext = new ModContext(modContainer);
        this.addContext(modContainer.getModId(), modContext);
        LibraOmni.LOGGER.info("Created context for [{}]", modContainer.getModId());
        return modContext;
    }

    public boolean existsForMod(String modId) {
        return this.contextMap.containsKey(modId);
    }

    public Set<ModContext> contexts() {
        return Set.copyOf(this.contextMap.values());
    }

    @Override
    public void listenToBus(IEventBus eventBus) {
        for (ModContext modContext : contexts()) {
            modContext.listenToBus(eventBus);
        }
    }

    private void addContext(String modId, ModContext modContext) {
        if (this.contextMap.containsKey(modId)) {
            throw new IllegalStateException("Context for [" + modId + "] already exists");
        }

        this.contextMap.put(modId, modContext);
    }
}
