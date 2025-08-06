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
    private final String name;
    private final String extension;
    /**
     * A directory relative to resource-set
     */
    private final String dir;

    public static final String JSON_EXT = "json";
    public static final String PNG_EXT = "png";

    private Resource(byte[] contents, String resourceDirectory, String name, String extension) {
        this.contents = contents;
        this.dir = resourceDirectory;
        this.name = name;
        this.extension = extension;
    }

    public boolean exists(Collection<String> resourceLocations) {
        return fileFromPath(resourceLocations, getPath()).isPresent();
    }

    public void saveToDisk(Filer filer) {
        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    getPath()
            );

            try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
                fileObjectWrite.write(getContents());
            }
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create resource [" + getPath() + "]", ioException);
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

    public static OutputFileBuilder readIfExists(Collection<String> resourceLocations) {
        return new OutputFileBuilder(resourceLocations);
    }

    public static OutputFileBuilder readIfExists(String... resourceLocations) {
        return readIfExists(List.of(resourceLocations));
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Resource other && this.getPath().equals(other.getPath());
    }

    public boolean contentEquals(Resource other) {
        return Arrays.equals(contents, other.contents);
    }

    public static class OutputFileBuilder {

        private final byte[] fileContents;
        private final Collection<String> resourceLocations;
        /**
         * A path relative to resource-set
         */
        private String filePath;
        private String fileName;
        private String fileExtension;

        private final boolean readIfExists;

        private OutputFileBuilder(Collection<String> resourceLocations) {
            this.fileContents = null;
            this.readIfExists = true;
            this.resourceLocations = resourceLocations;
        }

        private OutputFileBuilder(byte[] fileContents) {
            this.fileContents = fileContents;
            this.readIfExists = false;
            this.resourceLocations = Set.of();
        }

        public OutputFileBuilder copyMetadata(Resource resource) {
            this.filePath = resource.dir;
            this.fileName = resource.name;
            this.fileExtension = resource.extension;
            return this;
        }

        public OutputFileBuilder asset(String modId, String path) {
            return directory("assets/" + modId + "/" + path);
        }

        public OutputFileBuilder directory(String path) {
            this.filePath = path.endsWith("/") ? path : (path + "/");
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

        private String getRelativePath() {
            return filePath + fileName + '.' + fileExtension;
        }

        public OutputFileBuilder json() {
            return extension(JSON_EXT);
        }

        public OutputFileBuilder png() {
            return extension(PNG_EXT);
        }

        public Resource build() {
            byte[] contents;
            if (readIfExists) {
                Optional<File> file = fileFromPath(resourceLocations, getRelativePath());
                if (file.isEmpty()) {
                    throw new IllegalStateException("File [%s] does not exist".formatted(getRelativePath()));
                }
                try (InputStream inputStream = new FileInputStream(file.get())) {
                    contents = inputStream.readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                contents = fileContents;
            }

            return new Resource(contents, filePath, fileName, fileExtension);
        }
    }
}
