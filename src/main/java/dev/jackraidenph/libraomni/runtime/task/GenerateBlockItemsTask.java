package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.WithBlockItem;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.PropertiesPool;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public class GenerateBlockItemsTask extends SequentialRuntimeTask {

    @Override
    void processElement(ProxiedAnnotatedElement element, String elementId, ModContext modContext) {
        AnnotatedElement object = element.proxiedElement();

        DeferredHolder<Block, ? extends Block> holder = AutoRegisters.getHolder(modContext, object);

        DeferredHolder<Item, BlockItem> blockItem = AutoRegisters.registerBlockItem(
                modContext.modId(),
                holder,
                PropertiesPool.Util.getItemPropertiesForElement(element, modContext)
        );
        LibraOmni.LOGGER.info("Registered block item [{}]", blockItem);
    }

    @Override
    public boolean requireId() {
        return true;
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.CONSTRUCT;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(WithBlockItem.class);
    }

    @Override
    public Set<Class<? extends RuntimeTask>> dependsOn() {
        return Set.of(RegisterObjectsTask.class, GatherPropertiesTask.class);
    }
}
