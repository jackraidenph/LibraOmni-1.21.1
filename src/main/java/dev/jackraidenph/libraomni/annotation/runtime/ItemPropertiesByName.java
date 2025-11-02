package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.annotation.value.ValidatedExpression;
import dev.jackraidenph.libraomni.annotation.value.ValidatedExpression.Type;
import dev.jackraidenph.libraomni.compilation.validation.AnnotationsPresentValidator;
import dev.jackraidenph.libraomni.compilation.validation.HolderTypesValidator;
import dev.jackraidenph.libraomni.compilation.validation.TypesValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NeedsRuntimeProcessing
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})

@ValidatedExpression(
        type = Type.OR,
        value = {
                @Validated(value = TypesValidator.class, args = "net.minecraft.world.item.Item"),
                @Validated(value = HolderTypesValidator.class, args = "net.minecraft.world.item.Item"),
                @Validated(value = AnnotationsPresentValidator.class, args = "dev.jackraidenph.libraomni.annotation.runtime.GeneratesBlockItem")
        }
)
public @interface ItemPropertiesByName {
    String value();
}
