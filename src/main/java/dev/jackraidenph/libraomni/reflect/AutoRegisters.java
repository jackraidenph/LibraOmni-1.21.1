package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoRegisters extends AbstractModContextExtension {

    private final TypeSafeRegisterMap registersMap = new TypeSafeRegisterMap();
    private DeferredRegister.Blocks blocksRegister;
    private DeferredRegister.Items itemsRegister;

    private boolean registersRegistered = false;

    public AutoRegisters(ModContext modContext) {
        super(modContext);
    }

    public static AutoRegisters mod(String modId) {
        return LibraOmni.getModContextManager().getOrCreate(modId).getExtension(AutoRegisters.class);
    }

    @Override
    public void setupConstruct(FMLConstructModEvent event) {
        event.enqueueWork(this::initRegisters);
    }

    public void initRegisters() {
        if (this.registersRegistered) {
            throw new IllegalStateException("Registers for [" + this.getContext().modId() + "] were already initialized");
        }

        this.createBlockAndItemRegisters();

        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.getContext().modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
                LibraOmni.LOGGER.info("Created register [{}] for [{}]", deferredRegister.getRegistryName(), this.getContext().modId());
            }
        }
        this.registersRegistered = true;
    }

    private void createBlockAndItemRegisters() {
        String modId = getContext().modId();
        this.blocksRegister = DeferredRegister.createBlocks(modId);
        this.itemsRegister = DeferredRegister.createItems(modId);

        this.add(Block.class, blocksRegister);
        this.add(Item.class, itemsRegister);
    }

    public DeferredRegister.Items itemsRegister() {
        return this.itemsRegister;
    }

    public DeferredRegister.Blocks blocksRegister() {
        return this.blocksRegister;
    }

    private <T> void add(Class<T> clazz, DeferredRegister<T> register) {
        this.registersMap.put(clazz, register);
    }

    public <T> DeferredRegister<T> forClass(Class<T> clazz) {
        Class<T> superclass = this.tryFindSuperclass(this.registersMap.keySet(), clazz);
        if (superclass == null) {
            return null;
        }

        return this.registersMap.get(superclass);
    }

    private <T> Class<T> tryFindSuperclass(Set<Class<?>> classes, Class<T> child) {
        for (Class<?> superclass : classes) {
            if (superclass.isAssignableFrom(child)) {
                //Checked via isAssignableFrom
                //noinspection unchecked
                return (Class<T>) superclass;
            }
        }

        return null;
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
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
