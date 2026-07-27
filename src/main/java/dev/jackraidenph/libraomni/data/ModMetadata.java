package dev.jackraidenph.libraomni.data;

import com.google.gson.annotations.SerializedName;

public class ModMetadata {
    @SerializedName("data")
    private final ModAnnotatedData annotatedData = new ModAnnotatedData();

    public ModAnnotatedData getAnnotatedData() {
        return annotatedData;
    }
}
