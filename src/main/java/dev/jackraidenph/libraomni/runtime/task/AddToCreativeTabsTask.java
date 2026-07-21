package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.annotation.runtime.InCreativeTab;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.AutoCreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

public class AddToCreativeTabsTask extends SequentialRuntimeTask {

    @Override
    void processElement(ProxiedAnnotatedElement element, String elementId, ModContext modContext) {
        InCreativeTab annotation = element.getAnnotation(InCreativeTab.class);
        String namespace = annotation.namespace();
        String location = annotation.value();

        AutoCreativeModeTabs autoCreativeModeTabs = modContext.getExtension(AutoCreativeModeTabs.class);

        AnnotatedElement object = element.proxiedElement();
        Class<?> clazz = SafeReflectionUtil.selfOrReturnType(object, true);

        DeferredHolder<?, ?> holder = SafeReflectionUtil.tryCastToDeferredHolder(object);
        if (holder == null) {
            holder = AutoRegisters.entry(modContext.modId(), clazz, elementId);
        }

        if (!(holder.get() instanceof ItemLike itemLike)) {
            throw new IllegalStateException("Trying to add non-ItemLike entry to a creative tab: " + element);
        }

        autoCreativeModeTabs.add(namespace, location, itemLike);
    }

    @Override
    public boolean requireId() {
        return true;
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
