package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.Delegate;
import dev.jackraidenph.libraomni.annotation.value.KeyValue;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@GeneratesBlockModelData(parentModel = "block/cube_all")
@Composed
public @interface GeneratesCubeAllModel {

    @Delegate(annotation = GeneratesBlockModelData.class, attribute = "value", transformer = StringToAllTextureTransformer.class)
    String value() default "";

    class StringToAllTextureTransformer implements Function<Object, Object> {
        @Override
        public KeyValue apply(Object string) {
            return ProxyFactory.makeValueAnnotation(KeyValue.class, Map.of("key", "all", "value", string));
        }
    }
}
