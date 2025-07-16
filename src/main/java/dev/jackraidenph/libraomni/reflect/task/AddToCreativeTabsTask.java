package dev.jackraidenph.libraomni.reflect.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.InCreativeTab;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.TransitiveAnnotatedElement;
import dev.jackraidenph.libraomni.reflect.extension.AutoRegisters;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.reflect.ModContext;
import dev.jackraidenph.libraomni.reflect.extension.AutoCreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public class AddToCreativeTabsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<TransitiveAnnotatedElement> elements) {
        for (TransitiveAnnotatedElement e : elements) {
            InCreativeTab annotation = e.getAnnotation(InCreativeTab.class);
            String namespace = annotation.namespace();
            String location = annotation.value();

            AutoCreativeModeTabs autoCreativeModeTabs = modContext.getExtension(AutoCreativeModeTabs.class);

            AnnotatedElement annotatedElement = e.unwrap();
            String id = SafeReflectionUtil.idOrDefault(annotatedElement);
            Class<?> clazz = SafeReflectionUtil.selfOrReturnType(annotatedElement, true);

            DeferredHolder<?, ?> holder;
            if (
                    annotatedElement instanceof Field field
                            && Modifier.isStatic(field.getModifiers())
                            && UnsafeReflectionUtil.getValue(annotatedElement, null, false) instanceof DeferredHolder<?, ?> deferredHolder
            ) {
                holder = deferredHolder;
            } else {
                holder = AutoRegisters.entry(modContext.modId(), clazz, id);

                if (holder == null) {
                    LibraOmni.LOGGER.error("Failed to add {} to creative tab, deferred holder not found", e);
                    continue;
                }
            }

            if (!(holder.get() instanceof ItemLike itemLike)) {
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
