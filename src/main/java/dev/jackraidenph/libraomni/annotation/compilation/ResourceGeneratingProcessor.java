package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.StringJoiner;

abstract class ResourceGeneratingProcessor extends AbstractCompilationProcessor {

    public ResourceGeneratingProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    public Filer getFiler() {
        return this.processingEnvironment().getFiler();
    }

    @Override
    public final void finish(RoundEnvironment roundEnvironment) {
        Messager messager = this.processingEnvironment().getMessager();

        super.finish(roundEnvironment);

        Set<Resource> createdFiles = this.output(roundEnvironment);

        StringJoiner fileNames = new StringJoiner(", ", "[", "]");
        for (Resource resource : createdFiles) {
            if (this.resourceExists(resource)) {
                continue;
            }

            if (this.createResource(resource)) {
                fileNames.add(resource.path());
            }
        }

        messager.printNote("Files created during the run: " + fileNames);
    }

    public Set<Resource> output(RoundEnvironment roundEnvironment) {
        return Set.of();
    }

    public final boolean createResource(Resource resource) {
        Filer filer = this.getFiler();

        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    resource.path()
            );

            try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
                fileObjectWrite.write(resource.bytes());
            }
        } catch (IOException ioException) {
            messager().printError("Failed to create resource [" + resource.path() + "]:\n" + ioException.getLocalizedMessage());
            return false;
        }

        return true;
    }

    private boolean resourceExists(Resource resource) {
        try {
            return filer().getResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    resource.path()
            ).getLastModified() > 0;
        } catch (IOException ioException) {
            return false;
        }
    }
}
