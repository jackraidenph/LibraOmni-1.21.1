package dev.jackraidenph.libraomni.util.context;

import dev.jackraidenph.libraomni.LibraOmni;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModContext {

    private final ModContainer modContainer;
    private final TypeSafeRegisterMap registersMap = new TypeSafeRegisterMap();

    private DeferredRegister.Blocks blocksRegister;
    private DeferredRegister.Items itemsRegister;

    private boolean registersRegistered = false;

    private ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    public String modId() {
        return this.modContainer.getModId();
    }

    private void createBlockAndItemRegisters() {
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

    public <T> DeferredRegister<T> getRegister(Class<? extends T> clazz) {
        Class<T> superclass = this.tryFindSuperclass(this.registersMap.keySet(), clazz);
        if (superclass == null) {
            return null;
        }

        return this.registersMap.get(superclass);
    }

    private <T> Class<T> tryFindSuperclass(Set<Class<?>> classes, Class<? extends T> child) {
        for (Class<?> superclass : classes) {
            if (superclass.isAssignableFrom(child)) {
                //Checked via isAssignableFrom
                //noinspection unchecked
                return (Class<T>) superclass;
            }
        }

        return null;
    }

    private <T> void addRegister(Class<T> clazz, DeferredRegister<T> register) {
        this.registersMap.put(clazz, register);
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

        this.createBlockAndItemRegisters();

        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
            }
        }
        this.registersRegistered = true;
    }

    public static Builder builder(ModContainer modContainer) {
        return new Builder(modContainer);
    }

    //Builder
    @SuppressWarnings("unused")
    public static class Builder {
        private final ModContext modContext;

        public Builder(ModContainer modContainer) {
            this.modContext = new ModContext(modContainer);
        }

        public <T> Builder addRegister(Class<T> type, DeferredRegister<T> register) {
            this.modContext.addRegister(type, register);
            return this;
        }

        public ModContext build() {
            return this.modContext;
        }
    }

    private static class TypeSafeRegisterMap {
        private final Map<Class<?>, DeferredRegister<?>> classToRegisterMap = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <T> DeferredRegister<T> put(Class<T> clazz, DeferredRegister<T> register) {
            return (DeferredRegister<T>) this.classToRegisterMap.put(clazz, register);
        }

        @SuppressWarnings("unchecked")
        public <T> DeferredRegister<T> get(Class<T> clazz) {
            return (DeferredRegister<T>) this.classToRegisterMap.get(clazz);
        }

        public Set<Class<?>> keySet() {
            return this.classToRegisterMap.keySet();
        }

        public Collection<DeferredRegister<?>> values() {
            return this.classToRegisterMap.values();
        }
    }
}
