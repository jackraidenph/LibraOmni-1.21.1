package dev.jackraidenph.libraomni.annotation.info;

import java.lang.annotation.*;

/**
 * This annotation indicates that the annotation it's attached to generates files (assets, data, etc.)
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GeneratesFiles {
}
