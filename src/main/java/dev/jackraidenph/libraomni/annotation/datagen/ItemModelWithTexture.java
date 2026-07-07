package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@ArbitraryItemModelData
@Composed
public @interface ItemModelWithTexture {

    @Delegate(annotation = ArbitraryItemModelData.class, attribute = "value", transformer = StringToLayer0TextureTransformer.class)
    String value();

    class StringToLayer0TextureTransformer implements Function<Object, Object> {
        @Override
        public StringPair apply(Object string) {
            return ProxyFactory.makeValueAnnotation(StringPair.class, Map.of("key", "layer0", "value", string));
        }
    }
}