package dev.jackraidenph.libraomni.util.data;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataFileManager {

    private static Reader READER;

    public static final String FILE_ROOT = "META-INF/" + LibraOmni.MODID + "/";

    private MetadataFileManager() {

    }

    public static Reader getReader() {
        if (READER == null) {
            READER = new Reader();
        }

        return READER;
    }

    public static Writer newWriter(Filer filer) {
        return new Writer(filer);
    }

    public static class Reader {

        private final Map<String, Metadata> modMetadataCache = new HashMap<>();

        private Reader() {

        }

        public Metadata readModData(String modId) {
            if (this.modMetadataCache.containsKey(modId)) {
                return this.modMetadataCache.get(modId);
            }

            return this.readAllModData()
                    .stream()
                    .filter(modData -> modData.getModId().equals(modId))
                    .findFirst()
                    .orElse(null);
        }

        public Set<Metadata> findModsWithElementData() {
            if (!this.modMetadataCache.isEmpty()) {
                return this.modMetadataCache.values()
                        .stream()
                        .filter(modData -> modData.getElementDataPath() != null)
                        .collect(Collectors.toSet());
            }

            return this.readAllModData()
                    .stream()
                    .filter(modData -> modData.getElementDataPath() != null)
                    .collect(Collectors.toSet());
        }

        public Set<Metadata> readAllModData() {
            return ResourceUtilities.getResourcesAsStrings(MetadataFileManager.FILE_ROOT + Metadata.fileName())
                    .map(Metadata::deserialize)
                    .filter(Objects::nonNull)
                    .peek(metadata -> this.modMetadataCache.put(metadata.getModId(), metadata))
                    .collect(Collectors.toSet());
        }

        public ElementData readElementData(String modId) {
            Metadata metadata = this.readModData(modId);
            if (metadata != null) {
                return readElementDataFromLocation(metadata.getElementDataPath());
            }
            return null;
        }

        private static Metadata readModDataFromLocation(String resource) {
            try (InputStream inputStream = ResourceUtilities.openResourceStream(resource)) {
                return Metadata.deserialize(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static ElementData readElementDataFromLocation(String location) {
            try (InputStream byteInputStream = ResourceUtilities.openResourceStream(location)) {
                String str = new String(byteInputStream.readAllBytes(), StandardCharsets.UTF_8);
                return ElementData.fromJson(str);
            } catch (IOException e) {
                LibraOmni.LOGGER.error("Failed to read element data from [{}]", location);
                return null;
            }
        }
    }

    public static class Writer {

        private final Filer filer;

        private Writer(Filer filer) {
            this.filer = filer;
        }

        public FileObject writeElementData(ElementData data) throws IOException {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    elementDataResource(data)
            );

            try (OutputStream outputStream = fileObject.openOutputStream()) {
                data.output(outputStream);
            }

            return fileObject;
        }

        public FileObject writeMetadata(Metadata metadata) throws IOException {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    metadataResource()
            );

            try (OutputStream outputStream = fileObject.openOutputStream()) {
                metadata.output(outputStream);
            }

            return fileObject;
        }

        public static String elementDataResource(ElementData elementData) {
            return FILE_ROOT + elementData.fileName();
        }

        public static String metadataResource() {
            return FILE_ROOT + Metadata.fileName();
        }
    }
}
