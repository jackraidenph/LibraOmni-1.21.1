package dev.jackraidenph.libraomni.annotation.compile.impl;

import dev.jackraidenph.libraomni.annotation.compile.api.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.impl.AnnotationScanRoot;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;

public class AnnotationScanRootProcessor extends AbstractCompileTimeProcessor {

    private static final String MOD_DECLARING_ANNOTATION = "net.neoforged.fml.common.Mod";

    private final Map<String, String> packageToModId;

    public AnnotationScanRootProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
        this.packageToModId = new HashMap<>();
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        Set<Element> roots = new HashSet<>(
                roundEnvironment.getElementsAnnotatedWith(AnnotationScanRoot.class)
        );

        if (roots.isEmpty()) {
            Messager messager = this.getProcessingEnvironment().getMessager();
            for (Element element : roundEnvironment.getRootElements()) {
                if (element.getKind().equals(ElementKind.CLASS)) {
                    TypeElement typeElement = (TypeElement) element;
                    List<? extends AnnotationMirror> mirrors = typeElement.getAnnotationMirrors();
                    if (!mirrors.isEmpty()) {
                        for (AnnotationMirror mirror : mirrors) {
                            try {
                                if (mirror.getAnnotationType().toString().equals(MOD_DECLARING_ANNOTATION)) {
                                    String pkg = CompileTimeProcessor.packageOf(
                                                    this.getProcessingEnvironment(),
                                                    typeElement
                                            )
                                            .getQualifiedName()
                                            .toString();

                                    String modId = null;
                                    for (ExecutableElement executableElement : mirror.getElementValues().keySet()) {
                                        if (executableElement.getSimpleName().toString().equals("value")) {
                                            modId = mirror.getElementValues().get(executableElement).getValue().toString();
                                        }
                                    }

                                    if (modId == null) {
                                        throw new IllegalStateException();
                                    }

                                    this.packageToModId.put(pkg, modId);

                                    messager.printNote("Failed to find ScanRoot for " + modId + ", using @Mod annotation as a reference");

                                    return true;
                                }
                            } catch (IllegalStateException | NoSuchElementException noSuchElementException) {
                                throw new IllegalStateException("Found @Mod annotation, but somehow the value was not there?");
                            }
                        }
                    }
                }
            }
        }

        for (Element typeElement : roots) {
            AnnotationScanRoot annotation = typeElement.getAnnotation(AnnotationScanRoot.class);

            String pkg = CompileTimeProcessor.packageOf(
                            this.getProcessingEnvironment(),
                            typeElement
                    )
                    .getQualifiedName()
                    .toString();

            String modId = this.getModId(pkg);

            if (modId != null && !annotation.value().equals(modId)) {
                throw new IllegalStateException("""
                        Found overlapping scan roots.
                        [%s] at [%s] overlaps [%s] at [%s]
                        """.formatted(modId, this.getPackageOfModId(modId), annotation.value(), pkg));
            }

            this.packageToModId.put(pkg, annotation.value());
        }
        return true;
    }

    private String getPackageOfModId(String modId) {
        for (Entry<String, String> entry : this.packageToModId.entrySet()) {
            if (entry.getValue().equals(modId)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public String getModId(String packageString) {
        String modId = this.packageToModId.get(packageString);
        if (modId != null) {
            return modId;
        }

        for (Entry<String, String> rootToModId : this.packageToModId.entrySet()) {
            if (packageString.startsWith(rootToModId.getKey())) {
                return this.packageToModId.get(rootToModId.getKey());
            }
        }

        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of(
                AnnotationScanRoot.class
        );
    }
}
