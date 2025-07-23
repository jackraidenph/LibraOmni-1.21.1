package dev.jackraidenph.libraomni.processor;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.annotation.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.IsRuntimeTask;
import dev.jackraidenph.libraomni.data.ProjectMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

class CreateMetadataTask implements CompilationTask {

    private final ProjectMetadata projectMetadata = new ProjectMetadata();

    private static boolean isRuntimeAnnotation(TypeElement annotationTypeElement) {
        Retention retention = annotationTypeElement.getAnnotation(Retention.class);
        if (retention == null) {
            return false;
        }

        return retention.value().equals(RetentionPolicy.RUNTIME);
    }

    private static boolean isNotService(AnnotationMirror annotationMirror) {
        return mirrorIsNot(annotationMirror, Composed.class) && mirrorIsNot(annotationMirror, Target.class) && mirrorIsNot(annotationMirror, Retention.class);
    }

    private static boolean mirrorIsNot(AnnotationMirror mirror, Class<? extends Annotation> annotation) {
        return !((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(annotation.getName());
    }

    private static boolean needsRuntimeProcessing(AnnotatedConstruct annotated) {
        NeedsRuntimeProcessing needsProcessingDirect = annotated.getAnnotation(NeedsRuntimeProcessing.class);
        if (needsProcessingDirect != null) {
            return true;
        }

        Composed composed = annotated.getAnnotation(Composed.class);

        if (composed != null) {
            boolean recursiveNeedsProcessing = false;
            for (AnnotationMirror annotationMirror : annotated.getAnnotationMirrors()) {
                if (isNotService(annotationMirror)) {
                    recursiveNeedsProcessing = needsRuntimeProcessing(annotationMirror.getAnnotationType().asElement());
                    if (recursiveNeedsProcessing) {
                        return true;
                    }
                }
            }
            return recursiveNeedsProcessing;
        }

        return false;
    }

    //Find custom annotations that need to be processed at runtime
    private TypeElement[] findRuntimeAnnotations(RoundEnvironment roundEnvironment) {
        //See if the need to be processed in runtime
        return roundEnvironment
                //Get ALL round elements
                .getRootElements()
                .stream()
                //Get ALL the element's annotations
                .flatMap(e -> e.getAnnotationMirrors().stream())
                //Transform them to TypeElement-s
                .map(am -> (TypeElement) am.getAnnotationType().asElement())
                //Reject duplicates
                .distinct()
                .filter(CreateMetadataTask::needsRuntimeProcessing)
                //Check if the annotation actually has RUNTIME retention
                .filter(CreateMetadataTask::isRuntimeAnnotation)
                //Collect to array for later use in RoundEnvironment#getElementsAnnotatedWithAny
                .toArray(TypeElement[]::new);
    }

    @Override
    public Collection<Resource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        //Find annotations to use for processing runtime data
        TypeElement[] runtimeAnnotations = findRuntimeAnnotations(roundEnv);
        if (runtimeAnnotations.length > 0) {
            processingEnv.getMessager().printNote("Found runtime annotations " + Arrays.toString(runtimeAnnotations));
        }

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWithAny(runtimeAnnotations);
        for (Element e : annotatedElements) {
            if (e.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
                continue;
            }

            String modId = modLocator.forElement(e);
            if (modId == null) {
                continue;
            }

            projectMetadata.getOrCreateModMetadata(modId).getAnnotatedData().addElement(e);
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

        return Set.of();
    }

    @Override
    public Set<Resource> finish(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        return Set.of(
                Resource.json(projectMetadata)
                        .directory(ProjectMetadata.DIRECTORY)
                        .name(ProjectMetadata.FILE_ROOT)
                        .build()
        );
    }
}
