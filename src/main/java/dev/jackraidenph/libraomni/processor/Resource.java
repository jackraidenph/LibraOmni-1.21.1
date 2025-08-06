package dev.jackraidenph.libraomni.processor;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.jackraidenph.libraomni.common.CommonGson;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class Resource {

    private final byte[] contents;
    private final String nameRoot;
    private final String extension;
    /**
     * A directory relative to resource-set
     */
    private final String directory;

    public static final String JSON_EXT = "json";
    public static final String PNG_EXT = "png";

    private Resource(byte[] contents, String resourceDirectory, String nameRoot, String extension) {
        this.contents = contents;
        this.directory = resourceDirectory;
        this.nameRoot = nameRoot;
        this.extension = extension;
    }

    public boolean exists(Collection<String> resourceLocations) {
        return fileFromPath(resourceLocations, getFilePath()).isPresent();
    }

    public void saveToClassOutput(Filer filer) {
        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    getFilePath()
            );

            try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
                fileObjectWrite.write(getContents());
            }
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create resource [" + getFilePath() + "]", ioException);
        }
    }

    private static Optional<File> fileFromPath(Collection<String> resourceLocations, String relativePath) {
        return resourceLocations.stream()
                .map(resLoc -> Path.of(resLoc, relativePath).toFile())
                .filter(File::exists)
                .findFirst();
    }

    public byte[] getContents() {
        return contents;
    }

    public String getDirectory() {
        return directory;
    }

    public String getNameRoot() {
        return nameRoot;
    }

    public String getExtension() {
        return extension;
    }

    public String getFileName() {
        return getNameRoot() + "." + getExtension();
    }

    public String getFilePath() {
        return getDirectory() + getFileName();
    }

    public static ResourceBuilder raw(byte[] bytes) {
        return new ResourceBuilder(bytes);
    }

    public static ResourceBuilder string(String str, Charset charset) {
        return raw(str.getBytes(charset));
    }

    public static ResourceBuilder string(String str) {
        return string(str, StandardCharsets.UTF_8);
    }

    public static ResourceBuilder text(String text) {
        return string(text).setExtension("txt");
    }

    public static ResourceBuilder text(String text, Charset charset) {
        return string(text, charset).setExtension("txt");
    }

    private static boolean isValidJson(String input) {
        try (JsonReader reader = new JsonReader(new StringReader(input))) {
            reader.skipValue();
            return reader.peek() == JsonToken.END_DOCUMENT;
        } catch (IOException e) {
            return false;
        }
    }

    public static ResourceBuilder json(String rawJson) {
        if (!isValidJson(rawJson)) {
            throw new IllegalArgumentException("Malformed JSON");
        }

        return string(rawJson).setJsonExtension();
    }

    public static ResourceBuilder json(Object object) {
        //Validity check is not necessary
        return string(CommonGson.DEFAULT.toJson(object)).setJsonExtension();
    }

    public static ResourceBuilder png(RenderedImage image) {
        return image(image).setPngExtension();
    }

    public static ResourceBuilder png(Raster raster) {
        return raster(raster).setPngExtension();
    }

    public static ResourceBuilder raster(Raster raster) {
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (dataBuffer.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new UnsupportedOperationException("Failed to get raw byte contents for non-byte image buffer type");
        }
        return raw(((DataBufferByte) dataBuffer).getData());
    }

    public static ResourceBuilder image(RenderedImage bufferedImage) {
        return raster(bufferedImage.getData());
    }

    public static ResourceBuilder readIfExists(Collection<String> resourceLocations) {
        return new ResourceBuilder(resourceLocations);
    }

    public static ResourceBuilder readIfExists(String... resourceLocations) {
        return readIfExists(List.of(resourceLocations));
    }

    @Override
    public String toString() {
        return getFilePath();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Resource other && this.getFilePath().equals(other.getFilePath());
    }

    public boolean contentEquals(Resource other) {
        return Arrays.equals(contents, other.contents);
    }

    public static class ResourceBuilder {

        private final byte[] fileContents;
        private final Collection<String> resourceLocations;
        /**
         * A path relative to resource-set
         */
        private String fileDirectory;
        private String fileNameRoot;
        private String fileExtension;

        private final boolean readIfExists;

        private ResourceBuilder(Collection<String> resourceLocations) {
            this.fileContents = null;
            this.readIfExists = true;
            this.resourceLocations = resourceLocations;
        }

        private ResourceBuilder(byte[] fileContents) {
            this.fileContents = fileContents;
            this.readIfExists = false;
            this.resourceLocations = Set.of();
        }

        public ResourceBuilder copyFilePathFrom(Resource resource) {
            this.fileDirectory = resource.directory;
            this.fileNameRoot = resource.nameRoot;
            this.fileExtension = resource.extension;
            return this;
        }

        public ResourceBuilder setAssetDirectory(String modId, String path) {
            return setDirectory("assets/" + modId + "/" + path);
        }

        public ResourceBuilder setDirectory(String path) {
            this.fileDirectory = path.endsWith("/") ? path : (path + "/");
            return this;
        }

        public ResourceBuilder setNameRoot(String name) {
            this.fileNameRoot = name;
            return this;
        }

        public ResourceBuilder setExtension(String extension) {
            this.fileExtension = extension;
            return this;
        }

        private String filePath() {
            return fileDirectory + fileNameRoot + '.' + fileExtension;
        }

        public ResourceBuilder setJsonExtension() {
            return setExtension(JSON_EXT);
        }

        public ResourceBuilder setPngExtension() {
            return setExtension(PNG_EXT);
        }

        public Resource build() {
            byte[] contents;
            if (readIfExists) {
                Optional<File> file = fileFromPath(resourceLocations, filePath());
                if (file.isEmpty()) {
                    throw new IllegalStateException("File [%s] does not exist".formatted(filePath()));
                }
                try (InputStream inputStream = new FileInputStream(file.get())) {
                    contents = inputStream.readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                contents = fileContents;
            }

            return new Resource(contents, fileDirectory, fileNameRoot, fileExtension);
        }
    }
}
