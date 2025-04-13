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

    public ResourceGeneratingProcessor(ProcessingEnvironment processingEnvironment, String root) {
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

        if (createdFiles.isEmpty()) {
            messager.printNote("No files were created during the run of " + this.getClass().getSimpleName());
        } else {
            StringJoiner fileNames = new StringJoiner(", ", "[", "]");
            for (Resource resource : createdFiles) {

                try {
                    this.createResource(resource);
                } catch (IOException ioException) {
                    messager.printWarning("IOException caught while trying to create resource [" + resource.path() + "]:\n" + ioException.getLocalizedMessage());
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

    public final FileObject createResource(Resource resource) throws IOException {
        Filer filer = this.getFiler();

        FileObject fileObject = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "",
                resource.path()
        );

        try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
            fileObjectWrite.write(resource.bytes());
        }

        return fileObject;
    }

}
