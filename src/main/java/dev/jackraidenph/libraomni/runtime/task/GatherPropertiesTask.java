package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.annotation.runtime.PropertiesSupplier;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.LifecycleSetup.LifecycleStage;
import dev.jackraidenph.libraomni.runtime.ModContext;
import dev.jackraidenph.libraomni.runtime.extension.PropertiesPool;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

public class GatherPropertiesTask extends SequentialRuntimeTask {

    @Override
    void processElement(ProxiedAnnotatedElement element, String elementId, ModContext modContext) {
        String id = element.getAnnotation(PropertiesSupplier.class).value();
        PropertiesPool propertiesPool = modContext.getExtension(PropertiesPool.class);

        Method method = (Method) element.proxiedElement();
        Class<?> returnType = method.getReturnType();

        Object val = UnsafeReflectionUtil.getMethodValue(method, null);
        if (Item.Properties.class.isAssignableFrom(returnType)) {
            propertiesPool.addItemProperties(id, (Item.Properties) val);
        } else if (BlockBehaviour.Properties.class.isAssignableFrom(returnType)) {
            propertiesPool.addBlockProperties(id, (BlockBehaviour.Properties) val);
        } else {
            String obj = String.valueOf(element.proxiedElement().getClass());
            throw new UnsupportedOperationException("[%s] annotated with @PropertiesEntry, must be either Item.Properties or BlockBehaviour.Properties".formatted(obj));
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