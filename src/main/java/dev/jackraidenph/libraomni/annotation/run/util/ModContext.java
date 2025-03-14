package dev.jackraidenph.libraomni.annotation.run.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class ModContext {

    private final ModContainer modContainer;
    private final Map<Class<?>, DeferredRegister<?>> registersMap;

    private DeferredRegister.Blocks blocksRegister;
    private DeferredRegister.Items itemsRegister;

    private boolean registersRegistered = false;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;
        this.registersMap = new HashMap<>();

        this.initRegistries();
    }

    public String modId() {
        return this.modContainer.getModId();
    }

    private void initRegistries() {
        this.blocksRegister = DeferredRegister.createBlocks(modContainer.getModId());
        this.itemsRegister = DeferredRegister.createItems(modContainer.getModId());

        this.registersMap.put(Block.class, blocksRegister);
        this.registersMap.put(Item.class, itemsRegister);
    }

    public DeferredRegister.Items itemsRegister() {
        return this.itemsRegister;
    }

    public DeferredRegister.Blocks blocksRegister() {
        return this.blocksRegister;
    }

    @SuppressWarnings("unchecked")
    public <T> DeferredRegister<T> getRegister(Class<T> resourceKey) {
        return (DeferredRegister<T>) registersMap.get(resourceKey);
    }

    private <T> void addRegister(Class<T> resourceKey, DeferredRegister<T> register) {
        this.registersMap.put(resourceKey, register);
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public void initRegisters() {
        if (this.registersRegistered) {
            throw new IllegalStateException("Registers for [" + this.modId() + "] were already initialized");
        }

        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
            }
        }
        this.registersRegistered = true;
    }
}
