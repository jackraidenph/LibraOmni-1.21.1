package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;
import dev.jackraidenph.libraomni.util.ColorUtil.InterpolationMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface TextureWithPalete {
    /**
     * @return An existing texture used for generating a new one
     */
    String originalTexture();

    /**
     * @return A palette of ARGB colors to be applied to the existing texture
     */
    int[] palette();

    /**
     * @return If the supplied palette is smaller than the amount of colors in the original texture - evenly spaced interpolated palette is created
     */
    InterpolationMode usePaletteInterpolation() default InterpolationMode.LINEAR_OKLAB;
}
