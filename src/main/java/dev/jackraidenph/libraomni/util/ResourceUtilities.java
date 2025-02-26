package dev.jackraidenph.libraomni.util;

import dev.jackraidenph.libraomni.LibraOmni;

import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

public class ResourceUtilities {

    public static InputStream openResourceStream(String resourceLocation) {
        return LibraOmni.classLoader().getResourceAsStream(resourceLocation);
    }

    public static Stream<URL> getResources(String resourceLocation) {
        return LibraOmni.classLoader().resources(resourceLocation);
    }
}
