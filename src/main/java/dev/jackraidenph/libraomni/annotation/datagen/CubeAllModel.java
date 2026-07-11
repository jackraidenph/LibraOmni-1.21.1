package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.data.proxy.SyntheticAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;

@GeneratesFiles

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@ArbitraryBlockModelData(parentModel = "block/cube_all")
@Composed
public @interface CubeAllModel {

    @Replaces(in = ArbitraryBlockModelData.class, attribute = "value", transformer = StringToAllTextureTransformer.class)
    String value() default "";

    class StringToAllTextureTransformer implements Function<Object, Object> {
        @Override
        public StringPair apply(Object string) {
            return SyntheticAnnotation.create(StringPair.class, Map.of("key", "all", "value", string));
        }
    }
}
