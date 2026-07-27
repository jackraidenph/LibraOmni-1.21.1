package dev.jackraidenph.libraomni.compilation;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;

import java.lang.annotation.*;
import java.util.Set;

public abstract class CompileConstants {

    //A special-case set of meta-annotations that must not be considered transitively
    public static final Set<Class<? extends Annotation>> UNFOLD_UNSUPPORTED = Set.of(
            UnfoldsInto.class,
            Target.class,
            Retention.class,
            Inherited.class,
            Repeatable.class,
            Documented.class,
            NeedsRuntimeProcessing.class
    );

    public static final String NF_MOD_ANNOTATION_CLASS_NAME = "net.neoforged.fml.common.Mod";

    public static final String RESOURCE_LOCATIONS_OPTION = "libraomni.resources";
    public static final String CLASSPATH_OPTION = "libraomni.classpath";
    public static final String SOURCES_OPTION = "libraomni.sources";
    public static final String CONFIG_OPTION = "libraomni.config";
    public static final String ENABLE_BLACK_MAGIC_OPTION = "libraomni.blackmagic";
    public static final String DISABLE_CACHE_OPTION = "libraomni.disablecache";
}
