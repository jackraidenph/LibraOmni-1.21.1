package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.meta.IsRuntimeTask;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;
import dev.jackraidenph.libraomni.data.ProjectMetadata;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner14;
import java.util.*;

final class CreateMetadataTask implements CompilationTask {

    private final ProjectMetadata projectMetadata = new ProjectMetadata();

    @Override
    public void processRound(ModIdGetter modLocator, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        //Find annotations to use for processing runtime data
        RuntimeAnnotatedElementsScanner scanner = new RuntimeAnnotatedElementsScanner();
        for (Element e : roundEnv.getRootElements()) {
            scanner.scan(e);
        }

        Set<TypeElement> runtimeAnnotations = scanner.annotations;
        if (!runtimeAnnotations.isEmpty()) {
            processingEnv.getMessager().printNote("Found runtime annotations " + runtimeAnnotations);
        }

        for (Element e : scanner.elements) {
            if (e.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
                continue;
            }

            String modId = modLocator.forElement(e);
            if (modId == null) {
                continue;
            }

            projectMetadata.getOrCreateModMetadata(modId).getAnnotatedData().addElement(e, processingEnv.getElementUtils());
        }

        //Process user-defined runtime tasks
        for (Element e : roundEnv.getElementsAnnotatedWith(IsRuntimeTask.class)) {
            String name = ((TypeElement) e).getQualifiedName().toString();
            String modId = modLocator.forElement(e);
            if (modId == null) {
                processingEnv.getMessager().printWarning("Got runtime task [" + name + "], but failed to compute the owning mod");
                continue;
            }

            projectMetadata.getOrCreateModMetadata(modId).addRuntimeTask(name);
        }
    }

    @Override
    public void finish(ModIdGetter modLocator, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        resourceManager.save(
                ResourceIdentifier.builder()
                        .setDirectory(ProjectMetadata.DIRECTORY)
                        .setNameRoot(ProjectMetadata.FILE_ROOT)
                        .setJsonExtension()
                        .build(),
                projectMetadata
        );
    }

    private static class RuntimeAnnotatedElementsScanner extends ElementScanner14<Set<Element>, Void> {

        private final Set<Element> elements = new HashSet<>();
        private final Set<TypeElement> annotations = new HashSet<>();

        @Override
        public Set<Element> scan(Element e, Void nothing) {
            e.accept(this, null);
            if (e.getAnnotationMirrors().isEmpty()) {
                return elements;
            }
            for (AnnotationMirror mirror : e.getAnnotationMirrors()) {
                TypeElement typeElement = (TypeElement) mirror.getAnnotationType().asElement();
                if (needsRuntimeProcessing(typeElement)) {
                    if (!e.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
                        elements.add(e);
                    }
                    annotations.add(typeElement);
                }
            }


            return elements;
        }

        private static boolean needsRuntimeProcessing(AnnotatedConstruct annotated) {
            if (annotated.getAnnotation(NeedsRuntimeProcessing.class) != null) {
                return true;
            }

            if (annotated.getAnnotation(Composed.class) == null) {
                return false;
            }

            for (AnnotationMirror annotationMirror : annotated.getAnnotationMirrors()) {
                if (needsRuntimeProcessing(annotationMirror.getAnnotationType().asElement())) {
                    return true;
                }
            }
            return false;
        }
    }
}
