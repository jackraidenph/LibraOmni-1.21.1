package dev.jackraidenph.libraomni.compilation.util;

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

    public static final String JSON_EXT = "json";
    public static final String PNG_EXT = "png";

    private final byte[] contents;
    private final String nameRoot;
    private final String extension;
    /**
     * A directory relative to resource-set
     */
    private final String directory;
    private final boolean readFromFile;

    private Resource(byte[] contents, String resourceDirectory, String nameRoot, String extension, boolean readFromFile) {
        this.contents = contents;
        this.directory = resourceDirectory;
        this.nameRoot = nameRoot;
        this.extension = extension;
        this.readFromFile = readFromFile;
    }

    private static Optional<File> fileFromPath(Collection<String> resourceLocations, String relativePath) {
        return resourceLocations.stream()
                .map(resLoc -> Path.of(resLoc, relativePath).toFile())
                .filter(File::exists)
                .findFirst();
    }

    public boolean resourceExistsOnDisk(Collection<String> resourceLocations) {
        return fileFromPath(resourceLocations, getFilePath()).isPresent();
    }

    public boolean wasReadFromFile() {
        return readFromFile;
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

    public static ResourceBuilder builder() {
        return new ResourceBuilder();
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

        /**
         * A path relative to resource-set
         */
        private String fileDirectory;
        private String fileNameRoot;
        private String fileExtension;
        private byte[] fileContents;

        private ResourceBuilder() {
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

        public ResourceBuilder setRawBytes(byte[] bytes) {
            this.fileContents = bytes;
            return this;
        }

        public ResourceBuilder setUTF8Contents(String str, Charset charset) {
            return setRawBytes(str.getBytes(charset));
        }

        public ResourceBuilder setUTF8Contents(String str) {
            return setUTF8Contents(str, StandardCharsets.UTF_8);
        }

        public ResourceBuilder setTextContents(String text) {
            return setUTF8Contents(text).setExtension("txt");
        }

        public ResourceBuilder setTextContents(String text, Charset charset) {
            return setUTF8Contents(text, charset).setExtension("txt");
        }

        private static boolean isValidJson(String input) {
            try (JsonReader reader = new JsonReader(new StringReader(input))) {
                reader.skipValue();
                return reader.peek() == JsonToken.END_DOCUMENT;
            } catch (IOException e) {
                return false;
            }
        }

        public ResourceBuilder setJsonContents(String rawJson) {
            if (!isValidJson(rawJson)) {
                throw new IllegalArgumentException("Malformed JSON");
            }

            return setUTF8Contents(rawJson).setJsonExtension();
        }

        public ResourceBuilder setJsonContents(Object object) {
            //Validity check is not necessary
            return setUTF8Contents(CommonGson.DEFAULT.toJson(object)).setJsonExtension();
        }

        public ResourceBuilder setPngContents(RenderedImage image) {
            return image(image).setPngExtension();
        }

        public ResourceBuilder setPngContents(Raster raster) {
            return setRasterContents(raster).setPngExtension();
        }

        public ResourceBuilder setRasterContents(Raster raster) {
            DataBuffer dataBuffer = raster.getDataBuffer();
            if (dataBuffer.getDataType() != DataBuffer.TYPE_BYTE) {
                throw new UnsupportedOperationException("Failed to get raw byte contents for non-byte image buffer type");
            }
            return setRawBytes(((DataBufferByte) dataBuffer).getData());
        }

        public ResourceBuilder image(RenderedImage bufferedImage) {
            return setRasterContents(bufferedImage.getData());
        }

        public Optional<Resource> tryRead(String... resourceLocations) {
            return tryRead(List.of(resourceLocations));
        }

        public Optional<Resource> tryRead(Collection<String> resourceLocations) {
            Optional<File> file = fileFromPath(resourceLocations, filePath());
            if (file.isEmpty()) {
                return Optional.empty();
            }
            try (InputStream inputStream = new FileInputStream(file.get())) {
                fileContents = inputStream.readAllBytes();
                return Optional.of(build(true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Resource build(boolean readFromFile) {
            return new Resource(fileContents, fileDirectory, fileNameRoot, fileExtension, readFromFile);
        }

        public Resource build() {
            return build(false);
        }
    }
}
