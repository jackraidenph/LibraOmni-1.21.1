package dev.jackraidenph.libraomni.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CommonGson {
    public static final Gson DEFAULT = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
}
