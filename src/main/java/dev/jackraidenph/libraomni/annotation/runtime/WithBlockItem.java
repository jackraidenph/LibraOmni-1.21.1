package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.annotation.meta.ValidatedExpression;
import dev.jackraidenph.libraomni.annotation.meta.ValidatedExpression.Type;
import dev.jackraidenph.libraomni.compilation.validation.HolderTypesValidator;
import dev.jackraidenph.libraomni.compilation.validation.TypesValidator;

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
                @Validated(value = TypesValidator.class, args = "net.minecraft.world.level.block.Block"),
                @Validated(value = HolderTypesValidator.class, args = "net.minecraft.world.level.block.Block")
        }
)
public @interface WithBlockItem {
}
