package dev.jackraidenph.libraomni.annotation.compilation;

import net.neoforged.fml.common.Mod;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;

public class CompilationProcessorsManager extends AbstractProcessor {

    private final Set<CompilationProcessor> processors = new HashSet<>();
    private ModLocator modLocator = null;
    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.registerProcessors(processingEnv);
        super.init(processingEnv);
        this.modLocator = new ModLocator(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        this.modLocator.updateMap(roundEnvironment);

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
                    ? compilationProcessor.processRound(modLocator, roundEnvironment, this.processingEnv)
                    : compilationProcessor.finish(modLocator, roundEnvironment, this.processingEnv);

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

    private void registerProcessors(ProcessingEnvironment environment) {
        Set<Class<? extends CompilationProcessor>> registeredTypes = new HashSet<>();
        for (CompilationProcessor compilationProcessor : CompilationProcessorRegistry.instantiate(environment)) {
            Class<? extends CompilationProcessor> type = compilationProcessor.getClass();
            if (registeredTypes.contains(type)) {
                throw new IllegalArgumentException("Duplicate processor type");
            }
            this.processors.add(compilationProcessor);
            registeredTypes.add(type);
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

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    protected static class ModLocator {
        private final NavigableMap<String, String> rootToModId = new TreeMap<>();
        private final ProcessingEnvironment environment;

        ModLocator(ProcessingEnvironment processingEnvironment) {
            this.environment = processingEnvironment;
        }

        private Elements elementsUtils() {
            return this.environment.getElementUtils();
        }

        private void updateMap(RoundEnvironment roundEnvironment) {
            roundEnvironment.getElementsAnnotatedWith(Mod.class)
                    .forEach(e -> {
                        TypeElement modClass = (TypeElement) e;
                        Mod modAnnotation = modClass.getAnnotation(Mod.class);
                        String modId = modAnnotation.value();
                        if (modId == null) {
                            return;
                        }
                        String pkg = this.elementsUtils().getPackageOf(e).getQualifiedName().toString();
                        this.environment.getMessager().printNote("Locator found mod [" + modId + "] at [" + pkg + "]");
                        rootToModId.put(pkg, modId);
                    });
        }

        public String modId(String pkg) {
            Entry<String, String> entry = rootToModId.floorEntry(pkg);
            return entry == null ? null : entry.getValue();
        }

        public String modId(Element element) {
            String pkg = elementsUtils().getPackageOf(element).toString();
            return modId(pkg);
        }

        public Collection<String> mods() {
            return this.rootToModId.values();
        }
    }
}
