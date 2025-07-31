package dev.jackraidenph.libraomni.reflect.extension;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.exception.AlreadyInitializedException;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.reflect.ModContext;
import dev.jackraidenph.libraomni.reflect.VanillaRegistriesAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AutoRegisters extends AbstractModContextExtension {

    private final Map<ResourceKey<?>, DeferredHolder<?, ?>> entryCache = new WeakHashMap<>();

    private final Map<Class<?>, DeferredRegister<?>> registersMap = new HashMap<>();

    private boolean initialized = false;
    private DeferredRegister.Items items = null;
    private DeferredRegister.Blocks blocks = null;

    public static AutoRegisters mod(String modId) {
        return LibraOmni.getModContextManager().getOrCreate(modId).getExtension(AutoRegisters.class);
    }

    public static DeferredHolder<Item, BlockItem> blockItem(DeferredHolder<Block, ? extends Block> blockHolder) {
        return entry(blockHolder.getId().getNamespace(), Item.class, blockHolder.getId().getPath());
    }

    public static <T extends Block> DeferredHolder<Block, T> registerBlock(String modId, String id, Function<BlockBehaviour.Properties, T> func) {
        AutoRegisters autoRegisters = mod(modId);
        return autoRegisters.blocks().registerBlock(id, func);
    }

    public static <T extends Item> DeferredHolder<Item, T> registerItem(String modId, String id, Function<Item.Properties, T> func) {
        AutoRegisters autoRegisters = mod(modId);
        return autoRegisters.items().registerItem(id, func);
    }

    public static <R, T extends R> DeferredHolder<R, T> register(String modId, String id, Class<R> clazz, Supplier<T> supplier) {
        AutoRegisters autoRegisters = mod(modId);
        return autoRegisters.getOrCreateRegister(clazz).register(id, supplier);
    }

    public static <R, T extends R> DeferredHolder<R, T> entry(String modId, Class<T> element) {
        String id = SafeReflectionUtil.idOrDefault(element);
        //Supertype for child type T is later scanned for, so R will be at least T upon cast, or actual supertype
        //noinspection unchecked
        return (DeferredHolder<R, T>) entry(modId, element, id);
    }

    public static <R, T extends R> DeferredHolder<R, T> entry(String modId, Class<R> entryType, String id) {
        AutoRegisters autoRegisters = mod(modId);

        if (entryType == null) {
            throw new IllegalArgumentException("Entry supertype is null");
        }

        DeferredRegister<R> register = autoRegisters.forClass(entryType);
        if (register == null) {
            throw new IllegalStateException("Register for class [%s] doesn't exist".formatted(entryType));
        }

        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(modId, id);

        //noinspection unchecked
        DeferredHolder<R, T> holder = (DeferredHolder<R, T>) autoRegisters.entryCache.get(ResourceKey.create(register.getRegistryKey(), resourceLocation));
        if (holder != null) {
            return holder;
        }

        //noinspection unchecked
        Set<DeferredHolder<R, T>> holders = register.getEntries().stream()
                .filter(entry -> entry.is(resourceLocation))
                .map(e -> (DeferredHolder<R, T>) e)
                .collect(Collectors.toSet());

        if (holders.isEmpty()) {
            return DeferredHolder.create(register.getRegistryKey(), resourceLocation);
        }

        DeferredHolder<R, T> retrieved = holders.iterator().next();
        autoRegisters.entryCache.put(retrieved.getKey(), retrieved);

        return retrieved;
    }

    public static <R, T extends R> DeferredHolder<R, T> holder(ModContext modContext, AnnotatedElement element) {
        DeferredHolder<?, ?> holder;
        if (
                element instanceof Field field
                        && Modifier.isStatic(field.getModifiers())
                        && UnsafeReflectionUtil.getValue(element, null, false) instanceof DeferredHolder<?, ?> deferredHolder
        ) {
            holder = deferredHolder;
        } else {
            holder = AutoRegisters.entry(
                    modContext.modId(),
                    SafeReflectionUtil.selfOrReturnType(element, true),
                    SafeReflectionUtil.idOrDefault(element)
            );
        }
        //noinspection unchecked
        return (DeferredHolder<R, T>) holder;
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
            throw new AlreadyInitializedException();
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

    public <T> DeferredRegister<T> forClass(Class<T> clazz) {
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
