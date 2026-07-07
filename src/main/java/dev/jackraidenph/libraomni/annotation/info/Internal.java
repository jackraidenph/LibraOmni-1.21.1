package dev.jackraidenph.libraomni.annotation.info;

import java.lang.annotation.*;

/**
 * This annotation indicates that the annotation it's attached is intended for internal use and shouldn't be used by mods
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Internal {
}
