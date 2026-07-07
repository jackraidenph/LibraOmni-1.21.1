package dev.jackraidenph.libraomni.annotation.datagen;


import dev.jackraidenph.libraomni.annotation.meta.Validated;
import dev.jackraidenph.libraomni.annotation.meta.ValidatedExpression;
import dev.jackraidenph.libraomni.annotation.meta.ValidatedExpression.Type;
import dev.jackraidenph.libraomni.compilation.validation.HolderTypesValidator;
import dev.jackraidenph.libraomni.compilation.validation.TypesValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@ValidatedExpression(
        type = Type.OR,
        value = {
                @Validated(value = TypesValidator.class, args = "net.minecraft.world.level.block.Block"),
                @Validated(value = HolderTypesValidator.class, args = "net.minecraft.world.level.block.Block"),
        }
)
public @interface GeneratesDrops {
    /**
     * If not filled - drops itself
     */
    String value() default "";

    String silkTouchAlternative() default "";

    boolean mustSurviveExplosion() default true;

    int min() default 1;

    /**
     * -1 - same as min()
     */
    int minLimit() default -1;

    int max() default 1;

    /**
     * -1 - same as max()
     */
    int maxLimit() default -1;

    int fortuneBonus() default 0;
}
