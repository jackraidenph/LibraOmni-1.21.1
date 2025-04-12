package dev.jackraidenph.libraomni.annotation.compilation;

import net.neoforged.fml.common.Mod;

import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.*;
import java.util.Map.Entry;

public class CompilationProcessorsManager extends AbstractProcessor {

    private final Set<CompilationProcessor> processors = new HashSet<>();
    private ModLocator modLocator = null;
    private static CompilationProcessorsManager INSTANCE = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.registerProcessors(processingEnv);
        super.init(processingEnv);
        this.modLocator = new ModLocator(processingEnv);

        INSTANCE = this;
    }

    @Nullable
    protected ModLocator modLocator() {
        return this.modLocator;
    }

    @Nullable
    protected static CompilationProcessorsManager runningInstance() {
        return INSTANCE;
    }

    @Nullable
    protected static ModLocator runningModLocator() {
        CompilationProcessorsManager manager = runningInstance();
        if (manager == null) {
            return null;
        }
        return manager.modLocator();
    }

    @Override

    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        this.modLocator.updateMap(roundEnvironment);

        Messager messager = this.processingEnv.getMessager();

        for (CompilationProcessor compilationProcessor : this.processors) {
            if (roundEnvironment.processingOver()) {
                messager.printNote("Finishing " + compilationProcessor.getClass().getSimpleName() + "...");
                try {
                    compilationProcessor.finish(roundEnvironment);
                } catch (Exception processorException) {
                    messager.printError("There was an error finishing a compile processor:\n" + processorException.getLocalizedMessage());
                    return false;
                }
                continue;
            }

            messager.printNote("Invoking " + compilationProcessor.getClass().getSimpleName() + "...");
            try {
                compilationProcessor.processRound(roundEnvironment);
            } catch (Exception processorException) {
                messager.printError("There was an error during a round a compile processor:\n" + processorException.getLocalizedMessage());
                return false;
            }
        }

        return false;
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
