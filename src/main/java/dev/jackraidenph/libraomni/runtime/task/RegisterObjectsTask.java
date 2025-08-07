package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.Registered;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.AnnotationAccessor;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.extension.PropertiesPool;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;

public class RegisterObjectsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<AnnotationAccessor<AnnotatedElement>> elements) {
        for (AnnotationAccessor<AnnotatedElement> e : elements) {
            DeferredHolder<?, ?> registered = registerArbitrary(modContext, e);
            if ((e.annotatedObject() instanceof Field field) && DeferredHolder.class.isAssignableFrom(field.getType())) {
                tryInjectDeferredHolder(field, registered);
            }
        }
    }

    private static void tryInjectDeferredHolder(Field holderField, DeferredHolder<?, ?> holder) {
        int mods = holderField.getModifiers();
        if (!Modifier.isStatic(mods)) {
            throw new UnsupportedOperationException("""
                    Trying to inject [%s] into a non-static field [%s],
                    make it static
                    """.formatted(holder, holderField.getName()));
        }

        if (Modifier.isFinal(mods)) {
            throw new UnsupportedOperationException("""
                    Trying to inject [%s] into a final field [%s],
                    make it not final
                    """.formatted(holder, holderField.getName()));
        }

        try {
            holderField.setAccessible(true);
            holderField.set(null, holder);
        } catch (IllegalAccessException illegalAccessException) {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unchecked") //A lot of unchecked warnings are actually checked via Class#isAssignableFrom
    private static <T> DeferredHolder<? super T, T> registerArbitrary(ModContext modContext, AnnotationAccessor<AnnotatedElement> element) {
        String modId = modContext.modId();
        String id = SafeReflectionUtil.idOrDefault(element);
        String propertiesId = element.getAnnotation(Registered.class).propertiesId();

        AnnotatedElement tempObject = element.annotatedObject();
        final AnnotatedElement object;
        if (tempObject instanceof Field field && DeferredHolder.class.isAssignableFrom(field.getType())) {
            object = (Class<T>) SafeReflectionUtil.extractTypeArguments(tempObject)[1];
        } else {
            object = tempObject;
        }

        Class<T> clazz = (Class<T>) SafeReflectionUtil.selfOrReturnType(object);

        if (Block.class.isAssignableFrom(clazz)) {
            DeferredHolder<? super T, T> block = (DeferredHolder<? super T, T>) AutoRegisters.registerBlock(
                    modId,
                    id,
                    (props) -> nullFailingStaticInstantiate(object, getBlockProperties(propertiesId, modContext))
            );
            LibraOmni.LOGGER.info("Registered block [{}]", block);
            return block;
        } else if (Item.class.isAssignableFrom(clazz)) {
            DeferredHolder<? super T, T> item = (DeferredHolder<? super T, T>) AutoRegisters.registerItem(
                    modId,
                    id,
                    (props) -> nullFailingStaticInstantiate(object, getItemProperties(propertiesId, modContext))
            );
            LibraOmni.LOGGER.info("Registered item [{}]", item);
            return item;
        }

        DeferredHolder<? super T, T> holder = AutoRegisters.register(modId, id, clazz, () -> nullFailingStaticInstantiate(object));
        LibraOmni.LOGGER.info("Registered [{}] from [{}]", holder, element);
        return holder;
    }

    private static BlockBehaviour.Properties getBlockProperties(String id, ModContext modContext) {
        return modContext.getExtension(PropertiesPool.class).getBlockProperties(id);
    }

    private static Item.Properties getItemProperties(String id, ModContext modContext) {
        return modContext.getExtension(PropertiesPool.class).getItemProperties(id);
    }

    private static <T> T nullFailingStaticInstantiate(AnnotatedElement object, Object... args) {
        try {
            T created = UnsafeReflectionUtil.getValue(object, null, true, args);

            if (created == null) {
                throw new IllegalStateException("Failed to instantiate object from element [%s]".formatted(object.toString()));
            }
            return created;
        } catch (IllegalArgumentException illegalArgumentException) {
            if (SafeReflectionUtil.isExecutable(object)) {
                String actual = Arrays.toString(SafeReflectionUtil.getMethodParameters(object));
                String expected = Arrays.toString(SafeReflectionUtil.inferTypes(args));
                throw new IllegalStateException("Expected executable with parameters %s, got %s".formatted(expected, actual));
            }
            throw new IllegalStateException(illegalArgumentException);
        }
    }

    @Override
    public Set<Class<? extends RuntimeTask>> dependsOn() {
        return Set.of(GatherPropertiesTask.class);
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.CONSTRUCT;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(Registered.class);
    }
}
