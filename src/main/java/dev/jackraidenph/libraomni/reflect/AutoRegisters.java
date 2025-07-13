package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.Registered;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.StringUtilities;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class AutoRegisters extends AbstractModContextExtension {

    private final Map<String, DeferredHolder<?, ?>> entryCache = new WeakHashMap<>();

    private final Map<Class<?>, DeferredRegister<?>> registersMap = new HashMap<>();

    private boolean initialized = false;
    private DeferredRegister.Items items = null;
    private DeferredRegister.Blocks blocks = null;

    public static AutoRegisters mod(String modId) {
        return LibraOmni.getModContextManager().getOrCreate(modId).getExtension(AutoRegisters.class);
    }

    public static <T> Optional<DeferredHolder<T, ? extends T>> entry(String modId, AnnotatedElement element) {
        String className = SafeReflectionUtil.objectName(element);
        Registered registered = element.getAnnotation(Registered.class);
        String id = registered == null || registered.value().isBlank()
                ? StringUtilities.snakeCase(className)
                : registered.value();

        Class<?> clazz = SafeReflectionUtil.selfOrReturnType(element);

        return entry(modId, clazz, id);
    }

    public static <T> Optional<DeferredHolder<T, ? extends T>> entry(String modId, Class<?> entryType, String id) {
        AutoRegisters autoRegisters = mod(modId);

        //noinspection unchecked
        DeferredHolder<T, ? extends T> holder = (DeferredHolder<T, ? extends T>) autoRegisters.entryCache.get(id);
        if (holder != null) {
            return Optional.of(holder);
        }

        DeferredRegister<T> register = autoRegisters.forClass(entryType);
        if (register == null) {
            throw new IllegalStateException();
        }

        ResourceLocation resourceLocation = ResourceLocation.tryBuild(modId, id);
        if (resourceLocation == null) {
            throw new IllegalArgumentException("Bad ResourceLocation [%s]".formatted(modId + ":" + id));
        }

        Set<DeferredHolder<T, ? extends T>> holders = register.getEntries().stream()
                .filter(entry -> entry.is(resourceLocation))
                .collect(Collectors.toSet());

        if (holders.isEmpty()) {
            return Optional.empty();
        }

        DeferredHolder<T, ? extends T> retrieved = holders.iterator().next();
        autoRegisters.entryCache.put(id, retrieved);

        return Optional.of(retrieved);
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

    public DeferredRegister.Items items() {
        return items == null ? (DeferredRegister.Items) createRegister(Item.class) : items;
    }

    public DeferredRegister.Blocks blocks() {
        return blocks == null ? (DeferredRegister.Blocks) createRegister(Block.class) : blocks;
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
    }

    public <T> void add(Class<T> clazz, DeferredRegister<T> register) {
        this.registersMap.put(clazz, register);
    }

    public <T> DeferredRegister<T> forClass(Class<?> clazz) {
        if (Block.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (DeferredRegister<T>) blocks();
        } else if (Item.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (DeferredRegister<T>) items();
        }

        Class<T> superclass = SafeReflectionUtil.tryFindSuperclass(this.registersMap.keySet(), clazz);
        if (superclass == null) {
            return null;
        }

        //Should never throw by design
        //noinspection unchecked
        return (DeferredRegister<T>) this.registersMap.get(superclass);
    }

    public <T> DeferredRegister<T> getOrCreateRegister(Class<T> clazz) {
        DeferredRegister<T> register = forClass(clazz);
        if (register != null) {
            return register;
        } else {
            return createRegister(clazz);
        }
    }

    private <T> DeferredRegister<T> createRegister(Class<T> clazz) {
        DeferredRegister<T> created;

        if (Block.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            created = (DeferredRegister<T>) (blocks = DeferredRegister.createBlocks(modId()));
        } else if (Item.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            created = (DeferredRegister<T>) (items = DeferredRegister.createItems(modId()));
        } else {
            Entry<Class<T>, ResourceKey<Registry<T>>> resourceKeyPair = VanillaRegistriesAccess.getRegistryResourceKey(clazz);
            if (resourceKeyPair == null) {
                return null;
            }
            created = DeferredRegister.create(resourceKeyPair.getValue(), modId());
            this.registersMap.put(resourceKeyPair.getKey(), created);
        }
        created.register(eventBus());
        LibraOmni.LOGGER.info("Created register [{}] for [{}]", created.getRegistryName(), modId());
        return created;
    }

    private String modId() {
        return this.getContext().modId();
    }

    private IEventBus eventBus() {
        return getContext().modContainer().getEventBus();
    }
}
