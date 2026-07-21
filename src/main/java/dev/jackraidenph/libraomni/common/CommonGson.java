package dev.jackraidenph.libraomni.common;

import com.google.gson.*;
import dev.jackraidenph.libraomni.common.LootTableData.ArbitraryOptions;
import dev.jackraidenph.libraomni.data.reflect.AnnotatedReflectionData;
import dev.jackraidenph.libraomni.data.reflect.ReflectionDataTypeAdapters.AnnotatedReflectionDataDeserializer;
import dev.jackraidenph.libraomni.data.reflect.ReflectionDataTypeAdapters.AnnotatedReflectionDataSerializer;

public final class CommonGson {

    private CommonGson() {

    }

    public static final Gson DEFAULT = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(AnnotatedReflectionData.class, new AnnotatedReflectionDataSerializer())
            .registerTypeAdapter(AnnotatedReflectionData.class, new AnnotatedReflectionDataDeserializer())
            .registerTypeAdapter(ArbitraryOptions.class, new ArbitraryOptions.ArbitraryOptionsSerializer())
            .setPrettyPrinting()
            .create();
}
