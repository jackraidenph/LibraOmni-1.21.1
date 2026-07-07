package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.common.ColorUtil.InterpolationMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface TextureWithPalete {
    /**
     * If left blank - assumes the texture with the name of the element the annotation is applied to
     */
    String originalTexture() default "";

    /**
     * @return A suffix appended to the generated texture's file name
     */
    String newTexturesuffix() default "";

    /**
     * Specifies a palette of ARGB colors to be applied to the existing texture
     */
    int[] palette();

    /**
     * @return If the supplied palette is smaller than the amount of colors in the original texture - evenly spaced interpolated palette is created
     */
    InterpolationMode usePaletteInterpolation() default InterpolationMode.LINEAR_OKLAB;
}
