package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.Registered;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.ProxyAnnotatedElement;
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
import java.util.Set;

public class RegisterObjectsTask extends SequentialRuntimeTask {

    @Override
    public void processElement(ProxyAnnotatedElement element, String elementId, ModContext modContext) {
        DeferredHolder<?, ?> registered = registerArbitrary(element, elementId, modContext);
        if ((element.original() instanceof Field field) && DeferredHolder.class.isAssignableFrom(field.getType())) {
            UnsafeReflectionUtil.tryInject(field, registered);
        }
    }

    @SuppressWarnings("unchecked") //A lot of unchecked warnings are actually checked via Class#isAssignableFrom
    private static <T> DeferredHolder<? super T, T> registerArbitrary(ProxyAnnotatedElement element, String id, ModContext modContext) {
        String modId = modContext.modId();
        String propertiesId = element.getAnnotation(Registered.class).propertiesId();

        AnnotatedElement tempObject = element.original();
        //Gets later passed to lambdas
        final AnnotatedElement object;
        //If we are trying to inject into a DeferredHolder - treat it as if we tried to register a class
        //With class being the second type argument of DeferredHolder
        if (tempObject instanceof Field field && DeferredHolder.class.isAssignableFrom(field.getType())) {
            //From DeferredHolder<A, B extends A>, extract class of B
            //For example, DeferredHolder<Block, MyBlock> -> Class<MyBlock>
            object = (Class<T>) SafeReflectionUtil.extractTypeArguments(tempObject)[1];
        } else {
            object = tempObject;
        }

        Class<T> clazz = (Class<T>) SafeReflectionUtil.selfOrReturnType(object);

        //Handle Block registration special case
        if (Block.class.isAssignableFrom(clazz)) {
            DeferredHolder<? super T, T> block = (DeferredHolder<? super T, T>) AutoRegisters.registerBlock(
                    modId,
                    id,
                    (props) -> UnsafeReflectionUtil.instantiateStatic(object, getBlockProperties(propertiesId, modContext))
            );
            LibraOmni.LOGGER.info("Registered block [{}]", block);
            return block;
            //Handle Item registration special case
        } else if (Item.class.isAssignableFrom(clazz)) {
            DeferredHolder<? super T, T> item = (DeferredHolder<? super T, T>) AutoRegisters.registerItem(
                    modId,
                    id,
                    (props) -> UnsafeReflectionUtil.instantiateStatic(object, getItemProperties(propertiesId, modContext))
            );
            LibraOmni.LOGGER.info("Registered item [{}]", item);
            return item;
        }

        //Register everything else
        DeferredHolder<? super T, T> holder = AutoRegisters.register(modId, id, clazz, () -> UnsafeReflectionUtil.instantiateStatic(object));
        LibraOmni.LOGGER.info("Registered [{}] from [{}]", holder, element);
        return holder;
    }

    private static BlockBehaviour.Properties getBlockProperties(String id, ModContext modContext) {
        return modContext.getExtension(PropertiesPool.class).getBlockProperties(id);
    }

    private static Item.Properties getItemProperties(String id, ModContext modContext) {
        return modContext.getExtension(PropertiesPool.class).getItemProperties(id);
    }

    @Override
    public boolean requireId() {
        return true;
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
