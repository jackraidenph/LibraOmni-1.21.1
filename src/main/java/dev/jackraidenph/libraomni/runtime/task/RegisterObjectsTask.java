package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.Registered;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.extension.PropertiesPool;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Set;

public class RegisterObjectsTask extends SequentialRuntimeTask {

    @Override
    public void processElement(ProxiedAnnotatedElement element, String elementId, ModContext modContext) {
        DeferredHolder<?, ?> registered = registerArbitrary(element, elementId, modContext);
        if ((element.proxiedElement() instanceof Field field) && DeferredHolder.class.isAssignableFrom(field.getType())) {
            UnsafeReflectionUtil.tryInject(field, registered);
        }
    }

    @SuppressWarnings("unchecked") //A lot of unchecked warnings are actually checked via Class#isAssignableFrom
    private static <T> DeferredHolder<? super T, T> registerArbitrary(ProxiedAnnotatedElement element, String id, ModContext modContext) {
        String modId = modContext.modId();

        AnnotatedElement tempObject = element.proxiedElement();
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
                    (props) -> UnsafeReflectionUtil.instantiateStatic(
                            object,
                            PropertiesPool.Util.getBlockPropertiesForElement(element, modContext)
                    )
            );
            LibraOmni.LOGGER.info("Registered block [{}]", block);
            return block;
            //Handle Item registration special case
        } else if (Item.class.isAssignableFrom(clazz)) {
            DeferredHolder<? super T, T> item = (DeferredHolder<? super T, T>) AutoRegisters.registerItem(
                    modId,
                    id,
                    (props) -> UnsafeReflectionUtil.instantiateStatic(
                            object,
                            PropertiesPool.Util.getItemPropertiesForElement(element, modContext)
                    )
            );
            LibraOmni.LOGGER.info("Registered item [{}]", item);
            return item;
        }

        //Register everything else
        DeferredHolder<? super T, T> holder = AutoRegisters.register(modId, id, clazz, () -> UnsafeReflectionUtil.instantiateStatic(object));
        LibraOmni.LOGGER.info("Registered [{}] from [{}]", holder, element);
        return holder;
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
