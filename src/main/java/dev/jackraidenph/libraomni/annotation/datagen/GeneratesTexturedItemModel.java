package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;
import dev.jackraidenph.libraomni.annotation.value.KeyValue;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@GeneratesItemModelData
@Composed
public @interface GeneratesTexturedItemModel {

    @Delegate(annotation = GeneratesItemModelData.class, attribute = "value", transformer = StringToLayer0TextureTransformer.class)
    String value();

    class StringToLayer0TextureTransformer implements Function<Object, Object> {
        @Override
        public KeyValue apply(Object string) {
            return ProxyFactory.makeValueAnnotation(KeyValue.class, Map.of("key", "layer0", "value", string));
        }
    }
}