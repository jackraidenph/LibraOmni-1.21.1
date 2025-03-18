package dev.jackraidenph.libraomni.annotation.compile.util.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.LibraOmni;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public record Metadata(Set<ModData> data) {

    private static final Gson METADATA_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE_NAME_SUFFIX = "metadata";

    public Metadata {
        data = data == null ? Set.of() : data;
    }

    public static String fileName() {
        return LibraOmni.MODID + "." + FILE_NAME_SUFFIX + ".json";
    }

    public void output(OutputStream outputStream) throws IOException {
        String str = METADATA_GSON.toJson(this);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
    }

    public static Metadata deserialize(String str) {
        return METADATA_GSON.fromJson(str, Metadata.class);
    }

    public record ModData(String modId, String rootPackage, @Nullable String elementDataFile) {
    }
}
