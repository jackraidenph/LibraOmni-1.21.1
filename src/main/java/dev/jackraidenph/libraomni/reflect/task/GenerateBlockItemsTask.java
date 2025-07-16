package dev.jackraidenph.libraomni.reflect.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.GeneratesBlockItem;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.reflect.extension.AutoRegisters;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.reflect.ModContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public class GenerateBlockItemsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<TransitiveAnnotatedElement> elements) {
        for (TransitiveAnnotatedElement element : elements) {
            AnnotatedElement unwrapped = element.unwrap();

            DeferredHolder<Block, ? extends Block> holder = AutoRegisters.holder(modContext, unwrapped);
            if (holder == null) {
                throw new IllegalStateException("Failed to obtain Block holder from [%s]".formatted(unwrapped.toString()));
            }

            DeferredItem<?> blockItem = AutoRegisters.mod(modContext.modId()).items().registerSimpleBlockItem(
                    holder,
                    RegisterObjectsTask.itemProperties(SafeReflectionUtil.declaringOrSelf(unwrapped))
            );
            LibraOmni.LOGGER.info("Registered block item [{}]", blockItem.getId());
        }
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.CONSTRUCT;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(GeneratesBlockItem.class);
    }

    @Override
    public Set<Class<? extends RuntimeTask>> dependsOn() {
        return Set.of(RegisterObjectsTask.class);
    }
}
