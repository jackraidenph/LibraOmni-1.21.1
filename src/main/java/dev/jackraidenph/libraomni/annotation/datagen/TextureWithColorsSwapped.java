package dev.jackraidenph.libraomni.annotation.datagen;

import dev.jackraidenph.libraomni.annotation.info.GeneratesFiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@GeneratesFiles

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface TextureWithColorsSwapped {
    /**
     * @return An existing texture used for generating a new one
     */
    String originalTexture();

    /**
     * @return A palette of ARGB colors to be exchanged to the new palette. Must be the same size as the #newPalette()
     */
    int[] oldColors();

    /**
     * @return A palette of ARGB colors to be applied in place of the old palette. Must be the same size as the #oldPalette()
     */
    int[] newColors();
}
