package dev.jackraidenph.libraomni.annotation.compile.impl;

import dev.jackraidenph.libraomni.annotation.compile.api.CompilationProcessor;
import dev.jackraidenph.libraomni.annotation.impl.AnnotationScanRoot;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;

public class ScanRootProcessor extends AbstractCompilationProcessor {

    private static final String MOD_DECLARING_ANNOTATION = "net.neoforged.fml.common.Mod";

    private final Map<String, String> modPackages = new HashMap<>();

    public ScanRootProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    private void tryAddIdFromModAnnotation(Element typeElement, AnnotationMirror mirror) {
        Messager messager = this.getProcessingEnvironment().getMessager();

        String pkg = CompilationProcessor.qualifiedPackageName(this.getProcessingEnvironment(), typeElement);

        String modId = null;
        for (ExecutableElement executableElement : mirror.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals("value")) {
                modId = mirror.getElementValues().get(executableElement).getValue().toString();
            }
        }

        if (modId == null) {
            throw new IllegalStateException("Failed to get value from @Mod annotation");
        }

        this.modPackages.put(modId, pkg);

        messager.printNote("@Mod[" + modId + "] + annotation as scan root");
    }

    private boolean tryFindModAnnotation(Element element) {
        if (!element.getKind().equals(ElementKind.CLASS)) {
            return false;
        }

        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        if (mirrors.isEmpty()) {
            return false;
        }

        for (AnnotationMirror mirror : mirrors) {
            if (!mirror.getAnnotationType().toString().equals(MOD_DECLARING_ANNOTATION)) {
                continue;
            }

            try {
                this.tryAddIdFromModAnnotation(element, mirror);
                return true;
            } catch (IllegalStateException | NoSuchElementException noSuchElementException) {
                throw new IllegalStateException("Found @Mod annotation, but somehow the value was not there?");
            }
        }

        return false;
    }

    private void processScanRoots(Set<Element> scanRoots) {
        for (Element typeElement : scanRoots) {
            AnnotationScanRoot annotation = typeElement.getAnnotation(AnnotationScanRoot.class);

            String pkg = CompilationProcessor.qualifiedPackageName(this.getProcessingEnvironment(), typeElement);

            String modId = this.modIdFromPackage(pkg);

            if (modId != null && !annotation.value().equals(modId)) {
                throw new IllegalStateException("""
                        Found overlapping scan roots.
                        [%s] at [%s] overlaps [%s] at [%s]
                        """.formatted(modId, this.modPackages.get(modId), annotation.value(), pkg));
            }

            this.modPackages.put(annotation.value(), pkg);
        }
    }

    private void tryProcessModAnnotation(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getRootElements()) {
            boolean modAnnotationFound = this.tryFindModAnnotation(element);
            if (modAnnotationFound) {
                return;
            }
        }
    }

    @Override
    public boolean finish(RoundEnvironment roundEnvironment) {
        Set<Element> scanRoots = new HashSet<>(
                roundEnvironment.getElementsAnnotatedWith(AnnotationScanRoot.class)
        );

        if (scanRoots.isEmpty()) {
            this.tryProcessModAnnotation(roundEnvironment);
        } else {
            this.processScanRoots(scanRoots);
        }

        return true;
    }

    public String modIdFromPackage(String packageString) {
        for (Entry<String, String> idToPackage : this.modPackages.entrySet()) {
            if (packageString.startsWith(idToPackage.getValue())) {
                return idToPackage.getKey();
            }
        }

        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(
                AnnotationScanRoot.class
        );
    }
}
