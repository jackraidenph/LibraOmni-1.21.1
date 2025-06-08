package dev.jackraidenph.libraomni.annotation.compilation;

import net.neoforged.fml.common.Mod;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CompilationProcessorsManager extends AbstractProcessor {

    private final Set<CompilationProcessor> processors = new HashSet<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private int round = 0;

    protected CompilationProcessorsManager() {
        this.registerProcessors(processingEnv);
    }

    private void registerProcessors(ProcessingEnvironment environment) {
        this.processors.addAll(CompilationProcessorRegistry.getAll(environment.getMessager()));
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        this.modIdGetter.findMods(roundEnvironment, this.processingEnv.getMessager());

        Messager messager = this.processingEnv.getMessager();
        boolean finishing = roundEnvironment.processingOver();

        if (!finishing) {
            messager.printNote("Processing round " + round);
        }

        Set<Resource> createdResources = new HashSet<>();

        for (CompilationProcessor compilationProcessor : this.processors) {
            final String op = finishing ? "Processing" : "Finishing";

            messager.printNote(op + " [" + compilationProcessor.getClass().getSimpleName() + "]");

            Collection<Resource> output = !finishing
                    ? compilationProcessor.processRound(modIdGetter, roundEnvironment, this.processingEnv)
                    : compilationProcessor.finish(modIdGetter, roundEnvironment, this.processingEnv);

            createdResources.addAll(output);
        }

        if (finishing) {
            saveAllResourcesToDisk(createdResources);
        }

        this.round++;
        return false;
    }

    private void saveAllResourcesToDisk(Collection<Resource> resources) {
        for (Resource resource : resources) {
            if (resourceExists(resource)) {
                this.processingEnv.getMessager().printWarning("Resource [" + resource.getPath() + "] already exists, skipping");
                continue;
            }

            saveResourceToDisk(resource);
        }
    }

    public final boolean saveResourceToDisk(Resource resource) {
        Filer filer = this.processingEnv.getFiler();

        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    resource.getPath()
            );

            try (OutputStream fileObjectWrite = fileObject.openOutputStream()) {
                fileObjectWrite.write(resource.getContents());
            }
        } catch (IOException ioException) {
            this.processingEnv.getMessager().printError("Failed to create resource [" + resource.getPath() + "]:\n" + ioException.getLocalizedMessage());
            return false;
        }

        return true;
    }

    private boolean resourceExists(Resource resource) {
        try {
            return this.processingEnv.getFiler().getResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    resource.getPath()
            ).getLastModified() > 0;
        } catch (IOException ioException) {
            return false;
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                Mod.class.getName()
        );
    }
}
