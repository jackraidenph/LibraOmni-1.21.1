package dev.jackraidenph.libraomni.util.context;

import dev.jackraidenph.libraomni.LibraOmni;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModContextManager {

    private static ModContextManager INSTANCE;

    private final Map<String, ModContext> contextMap = new HashMap<>();

    private ModContextManager() {
    }

    public static ModContextManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModContextManager();
        }

        return INSTANCE;
    }

    public ModContext forMod(String modId) {
        if (ModList.get().getModContainerById(modId).isEmpty()) {
            throw new IllegalArgumentException("No such mod id exists [" + modId + "]");
        }

        if (!this.contextMap.containsKey(modId)) {
            throw new IllegalStateException("No context found for [" + modId + "]");
        }

        return this.contextMap.get(modId);
    }

    public ModContext newContext(ModContainer modContainer) {
        ModContext modContext = new ModContext(modContainer);
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
