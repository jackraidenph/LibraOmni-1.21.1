package dev.jackraidenph.libraomni.annotation.datagen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface GeneratesRecoloredTexture {
    /**
     * If left blank - assumes the texture with the name of the element the annotation is applied to
     *
     * @return An existing texture used for generating a new one
     */
    String parentTexture() default "";

    /**
     * @return A suffix appended to the generated texture's file name
     */
    String suffix() default "";

    /**
     * @return A palette of ARGB colors to be exchanged to the new palette. Must be the same size as the #newPalette()
     */
    int[] oldColors();

    /**
     * @return A palette of ARGB colors to be applied in place of the old palette. Must be the same size as the #oldPalette()
     */
    int[] newColors();
}
