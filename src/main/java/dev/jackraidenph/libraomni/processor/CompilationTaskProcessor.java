package dev.jackraidenph.libraomni.processor;

import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CompilationTaskProcessor extends AbstractProcessor {

    public static final String NF_MOD_ANNOTATION_CLASS_NAME = "net.neoforged.fml.common.Mod";

    private final Set<CompilationTask> tasks = new HashSet<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        CompilationTasks.init(this);
    }

    void registerTask(CompilationTask task) {
        if (this.tasks.stream().map(Object::getClass).anyMatch(clazz -> clazz.equals(task.getClass()))) {
            return;
        }

        this.tasks.add(task);
        if (processingEnv != null) {
            processingEnv.getMessager().printNote("Registered [" + task.getClass().getSimpleName() + "] for processing");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(NF_MOD_ANNOTATION_CLASS_NAME);
        this.modIdGetter.findMods(modAnnotation, roundEnvironment, this.processingEnv.getMessager());

        Messager messager = this.processingEnv.getMessager();
        boolean finishing = roundEnvironment.processingOver();

        if (!finishing) {
            messager.printNote("Processing round " + round);
        }

        Set<Resource> createdResources = new HashSet<>();

        for (CompilationTask compilationTask : this.tasks) {
            final String op = finishing ? "Finishing" : "Processing";

            messager.printNote(op + " [" + compilationTask.getClass().getSimpleName() + "]");

            try {
                RoundEnvironment proxyEnvironment = ProxyFactory.proxifyRuntimeEnvironment(roundEnvironment, processingEnv);
                Collection<Resource> output = !finishing
                        ? compilationTask.processRound(modIdGetter, proxyEnvironment, this.processingEnv)
                        : compilationTask.finish(modIdGetter, proxyEnvironment, this.processingEnv);
                createdResources.addAll(output);
            } catch (Exception e) {
                try (
                        StringWriter stringWriter = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(stringWriter)
                ) {
                    e.printStackTrace(printWriter);
                    messager.printNote(stringWriter.getBuffer());
                } catch (IOException ioException) {
                    throw new IllegalStateException(ioException);
                }
                throw new RuntimeException(
                        "Exception thrown while processing [%s]".formatted(compilationTask.getClass().getSimpleName()),
                        e
                );
            }
        }

        if (!createdResources.isEmpty()) {
            messager.printNote("Saving resources " + createdResources);
        }

        saveAllResourcesToDisk(createdResources);

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

    private void saveResourceToDisk(Resource resource) {
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
        }
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
        return Set.of(NF_MOD_ANNOTATION_CLASS_NAME);
    }
}