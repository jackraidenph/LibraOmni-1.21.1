package dev.jackraidenph.libraomni.annotation.compile.util.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.LibraOmni;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Metadata {

    private static final Gson METADATA_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String modId;

    private String elementDataPath = null;
    private Set<String> runtimeProcessors = null;
    private Set<String> compilationProcessors = null;

    public Metadata(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public String getElementDataPath() {
        return elementDataPath;
    }

    public Set<String> getRuntimeProcessors() {
        if (runtimeProcessors == null) {
            runtimeProcessors = new HashSet<>();
        }

        return runtimeProcessors;
    }

    public Set<String> getCompilationProcessors() {
        if (compilationProcessors == null) {
            compilationProcessors = new HashSet<>();
        }

        return compilationProcessors;
    }

    public void setElementDataPath(String path) {
        this.elementDataPath = path;
    }

    public void addRuntimeProcessorClass(String qualifiedName) {
        this.getRuntimeProcessors().add(qualifiedName);
    }

    public void addCompilationProcessorClass(String qualifiedName) {
        this.getCompilationProcessors().add(qualifiedName);
    }

    private static final String FILE_NAME_SUFFIX = "metadata";

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

}
