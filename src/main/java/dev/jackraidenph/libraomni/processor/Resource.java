package dev.jackraidenph.libraomni.processor;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.jackraidenph.libraomni.common.CommonGson;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Resource {

    private final byte[] contents;
    private final String name;
    private final String extension;
    /**
     * A path relative to resource-set
     */
    private final String dir;

    private Resource(byte[] contents, String resourceDirectory, String name, String extension) {
        this.contents = contents;
        this.dir = resourceDirectory.endsWith("/") ? resourceDirectory : (resourceDirectory + "/");
        this.name = name;
        this.extension = extension;
    }

    public byte[] getContents() {
        return contents;
    }

    public String getDirectory() {
        return dir;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String getFileName() {
        return getName() + "." + getExtension();
    }

    public String getPath() {
        return getDirectory() + getFileName();
    }

    public static OutputFileBuilder raw(byte[] bytes) {
        return new OutputFileBuilder(bytes);
    }

    public static OutputFileBuilder string(String str, Charset charset) {
        return raw(str.getBytes(charset));
    }

    public static OutputFileBuilder string(String str) {
        return string(str, StandardCharsets.UTF_8);
    }

    public static OutputFileBuilder text(String text) {
        return string(text).extension("txt");
    }

    public static OutputFileBuilder text(String text, Charset charset) {
        return string(text, charset).extension("txt");
    }

    private static boolean isValidJson(String input) {
        try (JsonReader reader = new JsonReader(new StringReader(input))) {
            reader.skipValue();
            return reader.peek() == JsonToken.END_DOCUMENT;
        } catch (IOException e) {
            return false;
        }
    }

    public static OutputFileBuilder json(String rawJson) {
        if (!isValidJson(rawJson)) {
            throw new IllegalArgumentException("Malformed JSON");
        }

        return string(rawJson).json();
    }

    public static OutputFileBuilder json(Object object) {
        //Validity check is not necessary
        return string(CommonGson.DEFAULT.toJson(object)).json();
    }

    public static OutputFileBuilder png(RenderedImage image) {
        return image(image).png();
    }

    public static OutputFileBuilder png(Raster raster) {
        return raster(raster).png();
    }

    public static OutputFileBuilder raster(Raster raster) {
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (dataBuffer.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new UnsupportedOperationException("Failed to get raw byte contents for non-byte image buffer type");
        }
        return raw(((DataBufferByte) dataBuffer).getData());
    }

    public static OutputFileBuilder image(RenderedImage bufferedImage) {
        return raster(bufferedImage.getData());
    }

    @Override
    public String toString() {
        return getPath();
    }

    public static class OutputFileBuilder {

        private final byte[] fileContents;
        /**
         * A path relative to resource-set
         */
        private String filePath;
        private String fileName;
        private String fileExtension;

        public OutputFileBuilder(byte[] fileContents) {
            this.fileContents = fileContents;
        }

        public OutputFileBuilder directory(String path) {
            this.filePath = path;
            return this;
        }

        public OutputFileBuilder name(String name) {
            this.fileName = name;
            return this;
        }

        public OutputFileBuilder extension(String extension) {
            this.fileExtension = extension;
            return this;
        }

        public OutputFileBuilder json() {
            return extension("json");
        }

        public OutputFileBuilder png() {
            return extension("png");
        }

        public Resource build() {
            return new Resource(fileContents, filePath, fileName, fileExtension);
        }

    }

}
