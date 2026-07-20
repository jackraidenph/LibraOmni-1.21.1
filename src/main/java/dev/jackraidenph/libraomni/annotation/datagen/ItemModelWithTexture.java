package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.data.proxy.runtime.SyntheticAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;


@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})

@GeneratesFiles
@UnfoldsInto(value = ArbitraryItemModelData.class, retainSelf = false)
public @interface ItemModelWithTexture {

    @Replaces(in = ArbitraryItemModelData.class, attribute = "value", transformer = StringToLayer0TextureTransformer.class)
    String value();

    class StringToLayer0TextureTransformer implements Function<String, StringPair> {
        @Override
        public StringPair apply(String string) {
            return SyntheticAnnotation.create(StringPair.class, Map.of("key", "layer0", "value", string));
        }
    }
}