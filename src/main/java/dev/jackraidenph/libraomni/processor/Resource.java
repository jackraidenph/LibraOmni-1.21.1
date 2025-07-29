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

public class Resource {

    private final byte[] contents;
    private final String name;
    private final String extension;
    /**
     * A path relative to resource-set
     */
    private final String dir;

    public static final String JSON_EXT = "json";
    public static final String PNG_EXT = "png";

    private Resource(byte[] contents, String resourceDirectory, String name, String extension) {
        this.contents = contents;
        this.dir = resourceDirectory.endsWith("/") ? resourceDirectory : (resourceDirectory + "/");
        this.name = name;
        this.extension = extension;
    }

    public boolean exists(Filer filer) {
        FileObject fileObject = fileObject(filer);
        return fileObject != null && fileObject.getLastModified() > 0;
    }

    public void saveToDisk(Filer filer) {
        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
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

    private static FileObject fileObjectFromPath(Filer filer, String dir, String file, String extension) {
        try {
            return filer.getResource(
                    StandardLocation.SOURCE_PATH,
                    "",
                    "resources/" + dir + file + '.' + extension
            );
        } catch (FileNotFoundException fileNotFoundException) {
            return null;
        } catch (IOException generalIO) {
            throw new IllegalStateException(generalIO);
        }
    }

    public FileObject fileObject(Filer filer) {
        return fileObjectFromPath(filer, getDirectory(), getName(), getExtension());
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

    public static OutputFileBuilder readIfExistsOrNull(Filer filer) {
        return new OutputFileBuilder(filer);
    }

    @Override
    public String toString() {
        return getPath();
    }

    public static class OutputFileBuilder {

        private final byte[] fileContents;
        private final Filer filer;
        /**
         * A path relative to resource-set
         */
        private String filePath;
        private String fileName;
        private String fileExtension;

        private final boolean readIfExists;

        private OutputFileBuilder(Filer filer) {
            this.fileContents = null;
            this.readIfExists = true;
            this.filer = filer;
        }

        private OutputFileBuilder(byte[] fileContents) {
            this.fileContents = fileContents;
            this.readIfExists = false;
            this.filer = null;
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
            byte[] contents;
            if (readIfExists) {
                if (filer == null) {
                    throw new IllegalArgumentException("Read if exists is set to true, but Filer is null");
                }

                FileObject fileObject = fileObjectFromPath(filer, filePath, fileName, fileExtension);
                if (fileObject == null) {
                    return null;
                }

                try (InputStream inputStream = fileObject.openInputStream()) {
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
