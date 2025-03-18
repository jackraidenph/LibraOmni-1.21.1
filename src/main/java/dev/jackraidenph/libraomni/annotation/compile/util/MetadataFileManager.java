package dev.jackraidenph.libraomni.annotation.compile.util;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.Metadata;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.Metadata.ModData;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataFileManager {

    private static Reader READER;

    public static final String FILE_ROOT = "META-INF/" + LibraOmni.MODID + "/";

    private MetadataFileManager() {

    }

    public static Reader reader() {
        if (READER == null) {
            READER = new Reader();
        }

        return READER;
    }

    public static Writer writer(Filer filer) {
        return new Writer(filer);
    }

    public static class Reader {

        private Reader() {

        }

        public Optional<ModData> readModData(String modId) {
            return this.readAllModData().stream().filter(modData -> modData.modId().equals(modId)).findFirst();
        }

        public Set<ModData> findModsWithElementData() {
            return this.readAllModData().stream().filter(modData -> modData.elementDataFile() != null).collect(Collectors.toSet());
        }

        public Set<ModData> readAllModData() {
            return ResourceUtilities.getResources(Metadata.fileName())
                    .map(url -> readModDataFromLocation(url.getPath()))
                    .filter(Objects::nonNull)
                    .flatMap(metadata -> metadata.data().stream())
                    .collect(Collectors.toSet());
        }

        public Optional<ElementData> readElementData(String modId) {
            return this.readModData(modId).map(modData -> readElementDataFromLocation(modData.elementDataFile()));
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
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class Writer {

        private final Filer filer;

        private Writer(Filer filer) {
            this.filer = filer;
        }

        public FileObject writeElementData(String modId, ElementData data) throws IOException {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    FILE_ROOT + ElementData.fileNameForMod(modId)
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
                    FILE_ROOT + Metadata.fileName()
            );

            try (OutputStream outputStream = fileObject.openOutputStream()) {
                metadata.output(outputStream);
            }

            return fileObject;
        }
    }
}
