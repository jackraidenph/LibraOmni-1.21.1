package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AutoRegisters extends AbstractModContextExtension {

    private final TypeSafeRegisterMap registersMap = new TypeSafeRegisterMap();

    private boolean initialized = false;

    public static AutoRegisters mod(String modId) {
        return LibraOmni.getModContextManager().getOrCreate(modId).getExtension(AutoRegisters.class);
    }

    public AutoRegisters(ModContext modContext) {
        super(modContext);
    }

    @Override
    public void setupConstruct(FMLConstructModEvent event) {
        event.enqueueWork(this::init);
    }

    @Override
    public void listenToBus(IEventBus eventBus) {
        eventBus.addListener(this::setupConstruct);
    }

    public void init() {
        if (this.initialized) {
            throw new IllegalStateException("Registers context for [" + this.getContext().modId() + "] were already initialized");
        }

        VanillaRegistriesAccess.mapAndCacheVanillaRegistries();

        this.initialized = true;
    }

    public DeferredRegister<Items> items() {
        return this.forClass(Items.class);
    }

    public DeferredRegister<Block> blocks() {
        return this.forClass(Block.class);
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
    }

    public <T> void add(Class<T> clazz, DeferredRegister<T> register) {
        this.registersMap.put(clazz, register);
    }

    public <T> DeferredRegister<T> forClass(Class<?> clazz) {
        Class<T> superclass = SafeReflectionUtil.tryFindSuperclass(this.registersMap.keySet(), clazz);
        if (superclass == null) {
            return null;
        }

        return this.registersMap.get(superclass);
    }

    protected <T> DeferredRegister<T> getOrCreateRegister(Class<T> clazz) {
        DeferredRegister<T> register = forClass(clazz);
        if (register != null) {
            return register;
        } else {
            DeferredRegister<T> created = createRegister(clazz);
            if (created == null) {
                return null;
            }
            LibraOmni.LOGGER.info("Created register [{}] for [{}]", created.getRegistryName(), modId());
            return created;
        }
    }

    private <T> DeferredRegister<T> createRegister(Class<T> clazz) {
        Entry<Class<T>, ResourceKey<Registry<T>>> resourceKeyPair = VanillaRegistriesAccess.getRegistryResourceKey(clazz);

        if (resourceKeyPair == null) {
            return null;
        }

        DeferredRegister<T> created = DeferredRegister.create(resourceKeyPair.getValue(), modId());
        created.register(eventBus());
        this.registersMap.put(resourceKeyPair.getKey(), created);

        return created;
    }

    private String modId() {
        return this.getContext().modId();
    }

    private IEventBus eventBus() {
        return getContext().modContainer().getEventBus();
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
