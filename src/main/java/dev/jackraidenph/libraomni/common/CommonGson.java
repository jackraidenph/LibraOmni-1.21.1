package dev.jackraidenph.libraomni.common;

import com.google.gson.*;
import dev.jackraidenph.libraomni.data.AnnotatedReflectionData;
import dev.jackraidenph.libraomni.data.GsonTypeAdapters.AnnotatedReflectionDataDeserializer;
import dev.jackraidenph.libraomni.data.GsonTypeAdapters.AnnotatedReflectionDataSerializer;

public class CommonGson {
    public static final Gson DEFAULT = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(AnnotatedReflectionData.class, new AnnotatedReflectionDataSerializer())
            .registerTypeAdapter(AnnotatedReflectionData.class, new AnnotatedReflectionDataDeserializer())
            .setPrettyPrinting()
            .create();
}
