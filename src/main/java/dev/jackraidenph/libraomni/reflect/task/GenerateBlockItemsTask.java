package dev.jackraidenph.libraomni.reflect.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.GeneratesBlockItem;
import dev.jackraidenph.libraomni.data.proxy.AnnotationAccessor;
import dev.jackraidenph.libraomni.reflect.extension.AutoRegisters;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.reflect.ModContext;
import dev.jackraidenph.libraomni.reflect.extension.PropertiesPool;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public class GenerateBlockItemsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<AnnotationAccessor<AnnotatedElement>> elements) {
        for (AnnotationAccessor<AnnotatedElement> element : elements) {
            AnnotatedElement object = element.annotatedObject();

            DeferredHolder<Block, ? extends Block> holder = AutoRegisters.holder(modContext, object);
            if (holder == null) {
                throw new IllegalStateException("Failed to obtain Block holder from [%s]".formatted(object.toString()));
            }

            String propertiesId = element.getAnnotationByClass(GeneratesBlockItem.class).propertiesId();
            Item.Properties properties = modContext.getExtension(PropertiesPool.class).getItemProperties(propertiesId);

            DeferredHolder<Item, BlockItem> blockItem = AutoRegisters.registerBlockItem(modContext.modId(), holder, properties);
            LibraOmni.LOGGER.info("Registered block item [{}]", blockItem);
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
