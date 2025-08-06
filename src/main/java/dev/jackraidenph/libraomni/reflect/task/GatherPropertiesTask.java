package dev.jackraidenph.libraomni.reflect.task;

import dev.jackraidenph.libraomni.annotation.PropertiesSupplier;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.AnnotationAccessor;
import dev.jackraidenph.libraomni.reflect.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.reflect.ModContext;
import dev.jackraidenph.libraomni.reflect.extension.PropertiesPool;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;

public class GatherPropertiesTask implements RuntimeTask {

    @Override
    public void process(ModContext modContext, Set<AnnotationAccessor<AnnotatedElement>> elements) {
        for (AnnotationAccessor<AnnotatedElement> e : elements) {
            String id = SafeReflectionUtil.idOrDefault(e);
            PropertiesPool propertiesPool = modContext.getExtension(PropertiesPool.class);

            Method method = (Method) e.annotatedObject();
            Class<?> returnType = method.getReturnType();

            Object val = UnsafeReflectionUtil.getMethodValue(method, null);
            if (Item.Properties.class.isAssignableFrom(returnType)) {
                propertiesPool.addItemProperties(id, (Item.Properties) val);
            } else if (BlockBehaviour.Properties.class.isAssignableFrom(returnType)) {
                propertiesPool.addBlockProperties(id, (BlockBehaviour.Properties) val);
            } else {
                String obj = String.valueOf(e.annotatedObject().getClass());
                throw new UnsupportedOperationException("[%s] annotated with @PropertiesEntry, must be either Item.Properties or BlockBehaviour.Properties".formatted(obj));
            }
        }
    }

    @Override
    public LifecycleStage getExecutionStage() {
        return LifecycleStage.CONSTRUCT;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(PropertiesSupplier.class);
    }
}