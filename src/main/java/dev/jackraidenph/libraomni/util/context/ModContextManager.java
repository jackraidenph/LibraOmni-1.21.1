package dev.jackraidenph.libraomni.util.context;

import dev.jackraidenph.libraomni.LibraOmni;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.*;

public enum ModContextManager {

    INSTANCE;

    private final Map<String, ModContext> contextMap = new HashMap<>();

    public ModContext getContext(String modId) {
        if (!this.contextMap.containsKey(modId)) {
            throw new IllegalStateException("No context found for [" + modId + "]");
        }

        return this.contextMap.get(modId);
    }

    public ModContext createContext(String modId) {
        ModList modList = ModList.get();
        Optional<? extends ModContainer> modContainerOptional = modList.getModContainerById(modId);
        if (modContainerOptional.isEmpty()) {
            throw new IllegalArgumentException("No ModContainer exists for [" + modId + "]");
        }

        ModContainer modContainer = modContainerOptional.get();
        return createContext(modContainer);
    }

    private ModContext createContext(ModContainer modContainer) {
        ModContext modContext = ModContext.builder(modContainer).build();
        this.addContext(modContainer.getModId(), modContext);
        LibraOmni.LOGGER.info("Created context for [{}]", modContainer.getModId());
        return modContext;
    }

    public boolean existsForMod(String modId) {
        return this.contextMap.containsKey(modId);
    }

    public Set<ModContext> contexts() {
        return new HashSet<>(this.contextMap.values());
    }

    private void addContext(String modId, ModContext modContext) {
        if (this.contextMap.containsKey(modId)) {
            throw new IllegalStateException("Context for [" + modId + "] already exists");
        }

        this.contextMap.put(modId, modContext);
    }
}
