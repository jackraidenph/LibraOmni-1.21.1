package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.BlockPropertiesSupplier;
import dev.jackraidenph.libraomni.annotation.HasBlockItem;
import dev.jackraidenph.libraomni.annotation.ItemPropertiesSupplier;
import dev.jackraidenph.libraomni.annotation.Registered;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RegisterObjectsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<TransitiveAnnotatedElement> elements) {
        for (TransitiveAnnotatedElement e : elements) {
            Class<?> clazz = selfOrReturnType(e);
            if (Block.class.isAssignableFrom(clazz)) {
                registerBlock(modContext, e, clazz);
            } else if (Item.class.isAssignableFrom(clazz)) {
                registerItem(modContext, e, clazz);
            } else {
                registerArbitrary(modContext, e, clazz);
            }
        }
    }

    private static <T> T getValueFromSingularMethod(Class<?> clazz, Predicate<Method> predicate, Supplier<T> defaultValue) {
        Set<Method> suppliers = Arrays.stream(clazz.getMethods())
                .filter(predicate)
                .collect(Collectors.toSet());
        if (suppliers.size() > 1) {
            throw new IllegalStateException("Found multiple matching methods for [%s]".formatted(clazz.getName()));
        } else if (suppliers.isEmpty()) {
            return defaultValue.get();
        }

        Method supplier = suppliers.iterator().next();

        return UnsafeReflectionUtil.getValue(supplier, null);
    }

    private static BlockBehaviour.Properties blockProperties(Class<?> blockClass) {
        return getValueFromSingularMethod(
                blockClass,
                m -> (m.getAnnotation(BlockPropertiesSupplier.class) != null)
                        && BlockBehaviour.Properties.class.isAssignableFrom(m.getReturnType())
                        && Modifier.isStatic(m.getModifiers())
                ,
                BlockBehaviour.Properties::of
        );
    }

    private static Item.Properties itemProperties(Class<?> blockOrItemClass) {
        return getValueFromSingularMethod(
                blockOrItemClass,
                m -> (m.getAnnotation(ItemPropertiesSupplier.class) != null)
                        && Item.Properties.class.isAssignableFrom(m.getReturnType())
                        && Modifier.isStatic(m.getModifiers())
                ,
                Item.Properties::new
        );
    }

    private static Class<?> selfOrReturnType(TransitiveAnnotatedElement element) {
        AnnotatedElement unwrapped = element.getAnnotatedElement();
        return switch (unwrapped) {
            case Class<?> clazz -> clazz;
            case Field field -> field.getType();
            case Method method -> method.getReturnType();
            case null, default -> throw new UnsupportedOperationException();
        };
    }

    private static void registerBlock(ModContext modContext, TransitiveAnnotatedElement blockElement, Class<?> hosting) {
        AutoRegisters register = modContext.getExtension(AutoRegisters.class);

        String id = SafeReflectionUtil.idOrDefault(blockElement);

        BlockBehaviour.Properties properties = blockProperties(hosting);

        DeferredBlock<?> block = register.blocks().registerBlock(
                id,
                (props) -> nullFailingStaticInstantiate(blockElement, props),
                properties
        );
        LibraOmni.LOGGER.info("Registered block [{}]", block.getId());

        if (blockElement.getAnnotation(HasBlockItem.class) != null) {
            Item.Properties itemProperties = itemProperties(hosting);
            registerBlockItem(itemProperties, block, register.items());
        }
    }

    private static void registerBlockItem(Item.Properties properties, DeferredBlock<?> block, DeferredRegister.Items items) {
        String id = block.getId().getPath();
        id += "_item";

        DeferredItem<?> blockItem = items.registerSimpleBlockItem(
                id,
                block,
                properties
        );
        LibraOmni.LOGGER.info("Registered block item [{}]", blockItem.getId());
    }

    private static void registerItem(ModContext modContext, TransitiveAnnotatedElement itemElement, Class<?> hosting) {
        AutoRegisters register = modContext.getExtension(AutoRegisters.class);

        String id = SafeReflectionUtil.idOrDefault(itemElement);

        Item.Properties properties = itemProperties(hosting);

        DeferredItem<?> item = register.items().registerItem(
                id,
                (props) -> nullFailingStaticInstantiate(itemElement, props),
                properties
        );
        LibraOmni.LOGGER.info("Registered item [{}]", item.getId());
    }

    private static <T> T nullFailingStaticInstantiate(AnnotatedElement element, Object... args) {
        T created = UnsafeReflectionUtil.getValue(element, null, args);
        if (created == null) {
            throw new IllegalStateException("Failed to instantiate object from element [%s]".formatted(element.toString()));
        }
        return created;
    }

    private static <T> void registerArbitrary(ModContext modContext, TransitiveAnnotatedElement element, Class<?> clazz) {
        if (clazz == null || Block.class.isAssignableFrom(clazz) || Item.class.isAssignableFrom(clazz)) {
            return;
        }

        AutoRegisters autoRegisters = modContext.getExtension(AutoRegisters.class);

        String id = SafeReflectionUtil.idOrDefault(element);

        //noinspection unchecked
        Class<T> genericClass = (Class<T>) clazz;
        DeferredRegister<? super T> register = autoRegisters.getOrCreateRegister(genericClass);
        if (register == null) {
            LibraOmni.LOGGER.warn("Failed to get register for [{}], skipping", genericClass.getSimpleName());
            return;
        }

        T toRegister = nullFailingStaticInstantiate(element);
        register.register(id, () -> toRegister);
        LibraOmni.LOGGER.info("Registered [{}:{}] to [{}] from [{}]",
                modContext.modId(),
                id,
                register.getRegistryName(),
                element
        );
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.CONSTRUCT;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(
                HasBlockItem.class,
                Registered.class
        );
    }
}
