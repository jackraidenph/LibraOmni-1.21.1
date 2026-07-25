package dev.jackraidenph.libraomni.gradle;

import java.util.Map;

public class LibraOmniExtension {

    public static String NAME = "libraOmni";

    public Map<String, String> annotationProcessorConfiguration = Map.of();
    public boolean blackMagicEnabled = false;
}
