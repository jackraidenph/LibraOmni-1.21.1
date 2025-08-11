package dev.jackraidenph.libraomni.compilation;

import dev.jackraidenph.libraomni.LibraOmni;

import java.util.Set;

public abstract class AnnotationProcessorConstants {
    public static final Set<String> PROCESSED_RESOURCES = Set.of(
            "data/*/tags/**",
            "assets/*/blockstatess/**",
            "assets/*/models/**"
    );

    public static final String NF_MOD_ANNOTATION_CLASS_NAME = "net.neoforged.fml.common.Mod";

    public static final String RESOURCE_LOCATIONS_OPTION = "resources";
    public static final String CONFIG_OPTION = "config";

    public static final String CONFIG_NAME = LibraOmni.MOD_ID + ".apconfig";
}
