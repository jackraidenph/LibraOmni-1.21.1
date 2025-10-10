package dev.jackraidenph.libraomni.compilation.util;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

public record ResourceIdentifier(String nameRoot, String extension, String directory) {

    public static final String JSON_EXT = "json";
    public static final String PNG_EXT = "png";

    private static final Location STANDARD_LOCATION = StandardLocation.CLASS_OUTPUT;

    public FileObject asFileObject(Filer filer) {
        return asFileObject(STANDARD_LOCATION, filer);
    }

    public FileObject asFileObject(Location location, Filer filer) {
        try {
            return filer.getResource(
                    location,
                    "",
                    getFilePath()
            );
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public Path fileObjectPath(Location location, Filer filer) {
        return Path.of(asFileObject(location, filer).toUri());
    }

    public Optional<Path> existsAt(Filer filer) {
        return existsAt(STANDARD_LOCATION, filer);
    }

    public Optional<Path> existsAt(Location location, Filer filer) {
        return existsAt(fileObjectPath(location, filer));
    }

    public Optional<Path> existsAt(Path... parentOrAbsolute) {
        return existsAt(List.of(parentOrAbsolute));
    }

    public Optional<Path> existsAt(Collection<Path> parentOrAbsolute) {
        return parentOrAbsolute.stream().filter(path -> resolvePath(path).toFile().exists()).findFirst();
    }

    public byte[] read(Filer filer) {
        return read(STANDARD_LOCATION, filer);
    }

    public byte[] read(Location location, Filer filer) {
        return read(fileObjectPath(location, filer));
    }

    public byte[] read(Path parentOrAbsolute) {
        try (InputStream is = inputStream(parentOrAbsolute)) {
            return is.readAllBytes();
        } catch (IOException ioException) {
            throw new RuntimeException("Exception reading resource [" + this + "]", ioException);
        }
    }

    public InputStream inputStream(Filer filer) throws IOException {
        return inputStream(STANDARD_LOCATION, filer);
    }

    public InputStream inputStream(Location location, Filer filer) throws IOException {
        return inputStream(fileObjectPath(location, filer));
    }

    private Path resolvePath(Path parentOrAbsolute) {
        return parentOrAbsolute.isAbsolute() && !parentOrAbsolute.toFile().isDirectory()
                ? parentOrAbsolute
                : parentOrAbsolute.resolve(getFilePath());
    }

    public InputStream inputStream(Path parentOrAbsolute) throws IOException {
        File file = resolvePath(parentOrAbsolute).toFile();
        return new FileInputStream(file);
    }

    public OutputStream outputStream(Filer filer) {
        return outputStream(STANDARD_LOCATION, filer);
    }

    public OutputStream outputStream(Location location, Filer filer) {
        return outputStream(fileObjectPath(location, filer));
    }

    public OutputStream outputStream(Path parentOrAbsolute) {
        try {
            File file = resolvePath(parentOrAbsolute).toFile();
            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            }
            return new FileOutputStream(file);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create resource [" + getFilePath() + "]", ioException);
        }
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
        return obj instanceof ResourceIdentifier other && this.getFilePath().equals(other.getFilePath());
    }

    public static class ResourceBuilder {

        /**
         * A path relative to resource-set
         */
        private String fileDirectory;
        private String fileNameRoot;
        private String fileExtension;

        private ResourceBuilder() {
        }

        public ResourceBuilder copyOf(ResourceIdentifier resourceIdentifier) {
            this.fileDirectory = resourceIdentifier.directory;
            this.fileNameRoot = resourceIdentifier.nameRoot;
            this.fileExtension = resourceIdentifier.extension;
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

        public ResourceBuilder setJsonExtension() {
            return setExtension(JSON_EXT);
        }

        public ResourceBuilder setPngExtension() {
            return setExtension(PNG_EXT);
        }

        public ResourceIdentifier build() {
            return new ResourceIdentifier(fileDirectory, fileNameRoot, fileExtension);
        }
    }
}
