package dev.jackraidenph.libraomni.data;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.CommonGson;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.AnnotatedElement;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class ModMetadataReader {

    @SuppressWarnings("FieldMayBeFinal")
    private static Filer COMPILATION_FILER = null;
    private ProjectMetadata projectMetadata = null;
    private boolean init = false;

    public String modIdOfElement(AnnotatedElement annotatedElement) {
        if (projectMetadata == null) {
            throw new IllegalStateException("Metadata file not read yet");
        }

        Map<String, ModMetadata> metadataMap = projectMetadata.getModMetadataMap();
        Optional<Entry<String, ModMetadata>> optional = metadataMap.
                entrySet()
                .stream()
                .filter(e -> e.getValue().getAnnotatedData().getElements().contains(annotatedElement))
                .findAny();

        return optional.map(Entry::getKey).orElse(null);

    }

    public void readMetadataFile() {
        try (InputStream inputStream = getProjectMetadataInputStream(COMPILATION_FILER)) {
            if (inputStream != null) {
                Reader nativeMetadataJson = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                this.projectMetadata = CommonGson.DEFAULT.fromJson(nativeMetadataJson, ProjectMetadata.class);
            } else {
                LibraOmni.LOGGER.error("Failed to fetch metadata file");
                this.projectMetadata = new ProjectMetadata();
            }

            init = true;
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private InputStream getProjectMetadataInputStream(Filer filer) throws IOException {
        if (filer != null) {
            ResourceIdentifier resourceIdentifier = ResourceIdentifier.builder()
                    .setDirectory(ProjectMetadata.DIRECTORY)
                    .setNameRoot(ProjectMetadata.FILE_ROOT)
                    .setJsonExtension()
                    .build();

            FileObject fo = resourceIdentifier.asFileObject(filer);
            return fo.openInputStream();
        } else {
            return openResourceStream(ProjectMetadata.PATH);
        }
    }

    private Map<String, ModMetadata> modMetadataMap() {
        if (!init) {
            throw new IllegalStateException("Reader was not initialized");
        }

        return this.projectMetadata.getModMetadataMap();
    }

    public ModMetadata getModMetadata(String modId) {
        return modMetadataMap().get(modId);
    }

    public Map<String, ModMetadata> getAllModMetadata() {
        return Collections.unmodifiableMap(modMetadataMap());
    }

    public Collection<String> getAllModsWithMetadata() {
        return modMetadataMap().keySet();
    }

    private static ClassLoader classLoader() {
        return ModMetadataReader.class.getClassLoader();
    }

    private static InputStream openResourceStream(String resourceLocation) {
        return classLoader().getResourceAsStream(resourceLocation);
    }
}
