package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.InCreativeTab;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.util.Set;

public class AddToCreativeTabsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<TransitiveAnnotatedElement> elements) {
        for (TransitiveAnnotatedElement e : elements) {
            InCreativeTab annotation = e.getAnnotation(InCreativeTab.class);
            String namespace = annotation.namespace();
            String location = annotation.value();

            AutoCreativeModeTabs autoCreativeModeTabs = modContext.getExtension(AutoCreativeModeTabs.class);

            DeferredHolder<?, ?> holder = AutoRegisters.entry(modContext.modId(), e.unwrap()).orElse(null);

            if (holder == null) {
                LibraOmni.LOGGER.error("Failed to add {} to creative tab, deferred holder not found", e);
                continue;
            }

            Object obj = holder.get();
            if (!(obj instanceof ItemLike itemLike)) {
                throw new IllegalStateException("Trying to add non-ItemLike entry to a creative tab: " + e);
            }

            autoCreativeModeTabs.add(namespace, location, itemLike);
        }
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.COMMON;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(InCreativeTab.class);
    }
}
