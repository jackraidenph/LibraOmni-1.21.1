package dev.jackraidenph.libraomni.compilation;

import java.util.*;

public abstract class AnnotationProcessorConstants {
    public static final Set<String> PROCESSED_RESOURCES = Set.of(
            "data/*/tags/**",
            "data/*/loot_table/**",
            "assets/*/blockstates/**",
            "assets/*/models/**",
            "assets/*/textures/**"
    );

    public static final String NF_MOD_ANNOTATION_CLASS_NAME = "net.neoforged.fml.common.Mod";

    public static final String RESOURCE_LOCATIONS_OPTION = "resources";
    public static final String CONFIG_OPTION = "config";
}
