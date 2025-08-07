package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.annotation.runtime.InCreativeTab;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.AnnotationAccessor;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.AutoCreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public class AddToCreativeTabsTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<AnnotationAccessor> elements) {
        for (AnnotationAccessor e : elements) {
            InCreativeTab annotation = e.getAnnotation(InCreativeTab.class);
            String namespace = annotation.namespace();
            String location = annotation.value();

            AutoCreativeModeTabs autoCreativeModeTabs = modContext.getExtension(AutoCreativeModeTabs.class);

            AnnotatedElement object = e.annotatedObject();
            String id = SafeReflectionUtil.idOrDefault(e);
            Class<?> clazz = SafeReflectionUtil.selfOrReturnType(object, true);

            DeferredHolder<?, ?> holder;
            if (
                    object instanceof Field field
                            && Modifier.isStatic(field.getModifiers())
                            && UnsafeReflectionUtil.getValue(object, null, false) instanceof DeferredHolder<?, ?> deferredHolder
            ) {
                holder = deferredHolder;
            } else {
                holder = AutoRegisters.entry(modContext.modId(), clazz, id);
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
