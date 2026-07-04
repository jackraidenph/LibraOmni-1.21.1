package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.common.ColorUtil.InterpolationMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface GeneratesPalettedTexture {
    /**
     * If left blank - assumes the texture with the name of the element the annotation is applied to
     */
    String parentTexture() default "";

    /**
     * @return A suffix appended to the generated texture's file name
     */
    String suffix() default "";

    /**
     * Specifies a palette of ARGB colors to be applied to the existing texture
     */
    int[] palette();

    /**
     * @return If the supplied palette is smaller than the amount of colors in the original texture - evenly spaced interpolated palette is created
     */
    InterpolationMode usePaletteInterpolation() default InterpolationMode.LINEAR_OKLAB;
}
