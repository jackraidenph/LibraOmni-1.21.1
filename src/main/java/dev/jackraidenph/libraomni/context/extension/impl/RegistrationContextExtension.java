package dev.jackraidenph.libraomni.context.extension.impl;

import dev.jackraidenph.libraomni.context.ModContext;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class RegistrationContextExtension extends AbstractModContextExtension {

    private final Map<ResourceKey<?>, DeferredRegister<?>> registersMap = new HashMap<>();
    private final DeferredRegister.Blocks blocksRegister;
    private final DeferredRegister.Items itemsRegister;

    public RegistrationContextExtension(ModContext modContext) {
        super(modContext);

        this.blocksRegister = DeferredRegister.createBlocks(modContext.modContainer().getModId());
        this.itemsRegister = DeferredRegister.createItems(modContext.modContainer().getModId());

        this.registersMap.put(blocksRegister.getRegistryKey(), blocksRegister);
        this.registersMap.put(itemsRegister.getRegistryKey(), itemsRegister);
    }

    public DeferredRegister.Items itemsRegister() {
        return this.itemsRegister;
    }

    public DeferredRegister.Blocks blocksRegister() {
        return this.blocksRegister;
    }

    @SuppressWarnings("unchecked")
    public <T> DeferredRegister<T> getDeferredRegister(ResourceKey<T> resourceKey) {
        return (DeferredRegister<T>) registersMap.get(resourceKey);
    }

    public <T> void addRegister(ResourceKey<T> resourceKey, DeferredRegister<T> register) {
        this.registersMap.put(resourceKey, register);
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
    }

    @Override
    public void onModConstruct() {
        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.getModContext().modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
            }
        }
    }
}
