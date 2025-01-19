package dev.jackraidenph.libraomni.annotation.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The following annotation marks the annotated class as the scan root for further compile-time annotation gathering.
 * If any found annotation exists under the package tree of the root, the annotation is dedicated to the specified Mod ID.
 * <br>
 * Multiple annotations can exist in the project, as long as they don't overlap; In this case, compile-time exception will be thrown.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ScanRoot {
    /**
     * @return Mod ID of the underlying scan targets.
     */
    String value();
}
