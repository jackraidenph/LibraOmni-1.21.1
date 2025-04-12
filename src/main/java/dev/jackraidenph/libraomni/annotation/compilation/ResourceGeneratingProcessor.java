package dev.jackraidenph.libraomni.annotation.compilation;

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
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

abstract class ResourceGeneratingProcessor extends AbstractCompilationProcessor {

    private final String[] rootElements;
    private static final Pattern ROOT_PATTERN_STRING = Pattern.compile("[A-z_]([\\-\\w]+/)+");

    public ResourceGeneratingProcessor(ProcessingEnvironment processingEnvironment, String root) {
        super(processingEnvironment);

        if (!checkRoot(root)) {
            throw new IllegalArgumentException("Illegal root string, a root should look like: aA/bA0/...cC0/");
        }

        this.rootElements = root.split("/");
    }

    private static boolean checkRoot(String toCheck) {
        return ROOT_PATTERN_STRING.matcher(toCheck).matches();
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

        Set<Resource> createdFiles = this.output(roundEnvironment);

        if (createdFiles.isEmpty()) {
            messager.printNote("No files were created during the run of " + this.getClass().getSimpleName());
        } else {
            StringJoiner fileNames = new StringJoiner(", ", "[", "]");
            for (Resource resource : createdFiles) {

                try {
                    this.createResource(
                            resource.path(),
                            new ByteArrayInputStream(resource.bytes())
                    );
                } catch (IOException ioException) {
                    messager.printError(ioException.getLocalizedMessage());
                    messager.printError(
                            "IOException caught trying to create resource [" + resource.path() + "] at " + this.getRoot()
                    );
                    continue;
                }

                fileNames.add(resource.path());
            }
            messager.printNote("Files created during the run: " + fileNames);
        }
    }

    public Set<Resource> output(RoundEnvironment roundEnvironment) {
        return Set.of();
    }

    public String outputLocation() {
        return this.getRoot();
    }

    public final FileObject createResource(String fileName, InputStream contents) throws IOException {
        Filer filer = this.getFiler();

        FileObject fileObject = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "",
                this.outputLocation() + fileName
        );

        try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
            contents.transferTo(fileObjectWrite);
        }

        return fileObject;
    }

}
