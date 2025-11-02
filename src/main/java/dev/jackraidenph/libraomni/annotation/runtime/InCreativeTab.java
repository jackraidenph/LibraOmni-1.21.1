package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.annotation.value.ValidatedExpression;
import dev.jackraidenph.libraomni.annotation.value.ValidatedExpression.Type;
import dev.jackraidenph.libraomni.compilation.validation.HolderTypeValidator;
import dev.jackraidenph.libraomni.compilation.validation.TypeValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@NeedsRuntimeProcessing

@ValidatedExpression(
        type = Type.OR,
        value = {
                @Validated(value = TypeValidator.class, args = "net.minecraft.world.level.ItemLike"),
                @Validated(value = HolderTypeValidator.class, args = "net.minecraft.world.level.ItemLike")
        }
)
public @interface InCreativeTab {
    String namespace() default "minecraft";

    String value();
}
