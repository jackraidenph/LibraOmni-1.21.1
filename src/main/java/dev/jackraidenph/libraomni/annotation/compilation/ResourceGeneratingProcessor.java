package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.StringJoiner;

public abstract class ResourceGeneratingProcessor extends AbstractCompilationProcessor {

    private final String[] rootElements;
    private static final String ROOT_PATTERN_STRING = "[A-z_]([\\-\\w]+/)+";

    public ResourceGeneratingProcessor(ProcessingEnvironment processingEnvironment, String root) {
        super(processingEnvironment);

        if (!checkRoot(root)) {
            throw new IllegalArgumentException("Illegal root string, a root should look like: aA/bA0/...cC0/");
        }

        this.rootElements = root.split("/");
    }

    private static boolean checkRoot(String toCheck) {
        return toCheck.matches(ROOT_PATTERN_STRING);
    }

    public final String[] getRootElements() {
        return this.rootElements;
    }

    public final String getRoot() {
        StringJoiner stringJoiner = new StringJoiner("/", "", "/");
        for (String e : this.getRootElements()) {
            stringJoiner.add(e);
        }
        return stringJoiner.toString();
    }

    public Filer getFiler() {
        return this.processingEnvironment().getFiler();
    }

    @Override
    public final void finish(RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnvironment().getMessager();

        super.finish(roundEnvironment);

        Set<TransientResource> createdFiles = this.output(roundEnvironment);

        if (createdFiles.isEmpty()) {
            messager.printNote("No files were created during the run of " + this.getClass().getSimpleName());
        } else {
            StringJoiner fileNames = new StringJoiner(", ", "[", "]");
            for (TransientResource resource : createdFiles) {

                try {
                    this.createResource(
                            resource.fullName(),
                            new ByteArrayInputStream(resource.bytes())
                    );
                } catch (IOException ioException) {
                    messager.printError(ioException.getLocalizedMessage());
                    messager.printError(
                            "IOException caught trying to create resource [" + resource.fullName() + "] at " + this.getRoot()
                    );
                    continue;
                }

                fileNames.add(resource.fullName());
            }
            messager.printNote("Files created during the run: " + fileNames);
        }
    }

    public Set<TransientResource> output(RoundEnvironment roundEnvironment) {
        return Set.of();
    }

    public final FileObject createResource(String fileName, InputStream contents) throws IOException {
        Filer filer = this.getFiler();

        FileObject fileObject = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "",
                this.getRoot() + fileName
        );

        try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
            contents.transferTo(fileObjectWrite);
        }

        return fileObject;
    }

    public record TransientResource(String name, String extension, byte[] bytes) {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        public TransientResource(String name, String extension, String stringContents) {
            this(name, extension, stringContents.getBytes(StandardCharsets.UTF_8));
        }

        public static TransientResource json(String name, String jsonString) {
            return new TransientResource(name, "json", jsonString);
        }

        public static TransientResource json(String name, Object jsonObject) {
            return json(name, GSON.toJson(jsonObject));
        }

        public static TransientResource png(String name, byte[] contents) {
            return new TransientResource(name, "png", contents);
        }

        public static TransientResource fullName(String nameWithExtension, byte[] contents) {
            int dotIndex = nameWithExtension.lastIndexOf(".");
            if (dotIndex < 0) {
                throw new IllegalArgumentException("Full file name must contain its extension");
            }

            String extension = nameWithExtension.substring(dotIndex + 1);
            String name = nameWithExtension.substring(0, dotIndex);

            return new TransientResource(name, extension, contents);
        }

        public static TransientResource fullName(String nameWithExtension, String contents) {
            return fullName(nameWithExtension, contents.getBytes(StandardCharsets.UTF_8));
        }

        public String fullName() {
            return this.name() + "." + this.extension();
        }
    }
}
