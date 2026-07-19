package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.data.proxy.SyntheticAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;


@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})

@GeneratesFiles
@UnfoldsInto(value = ArbitraryBlockModelData.class, retainSelf = false)
public @interface CubeAllModel {

    @Replaces(in = ArbitraryBlockModelData.class, attribute = "value", transformer = StringToAllTextureTransformer.class)
    String value() default "";

    @Replaces(in = ArbitraryBlockModelData.class, attribute = "parentModel")
    String parentModel() default "block/cube_all";

    class StringToAllTextureTransformer implements Function<String, StringPair> {
        @Override
        public StringPair apply(String string) {
            return SyntheticAnnotation.create(StringPair.class, Map.of("key", "all", "value", string));
        }
    }
}
