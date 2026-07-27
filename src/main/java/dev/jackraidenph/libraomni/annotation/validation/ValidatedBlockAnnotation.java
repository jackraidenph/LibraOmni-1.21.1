package dev.jackraidenph.libraomni.annotation.validation;

import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.annotation.validation.ValidatedExpression.Type;
import dev.jackraidenph.libraomni.compilation.validation.HolderTypesValidator;
import dev.jackraidenph.libraomni.compilation.validation.TypesValidator;

@UnfoldsInto(value = ValidatedExpression.class, retainSelf = false)
public @interface ValidatedBlockAnnotation {
    @Replaces(attribute = "type", in = ValidatedExpression.class)
    Type type() default Type.OR;

    @Replaces(attribute = "value", in = ValidatedExpression.class)
    Validated[] value() default {
            @Validated(value = TypesValidator.class, args = "net.minecraft.world.level.block.Block"),
            @Validated(value = HolderTypesValidator.class, args = "net.minecraft.world.level.block.Block"),
    };
}
