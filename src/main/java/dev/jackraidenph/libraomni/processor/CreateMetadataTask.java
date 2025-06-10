package dev.jackraidenph.libraomni.processor;

import dev.jackraidenph.libraomni.annotation.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.annotation.RegisteredRuntimeTask;
import dev.jackraidenph.libraomni.common.data.NativeMetadata;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

class CreateMetadataTask implements CompilationTask {

    private final NativeMetadata nativeMetadata = new NativeMetadata();

    private static boolean isRuntimeAnnotation(TypeElement annotationTypeElement) {
        Retention retention = annotationTypeElement.getAnnotation(Retention.class);
        if (retention == null) {
            return false;
        }

        return retention.value().equals(RetentionPolicy.RUNTIME);
    }

    //Find custom annotations that need to be processed at runtime
    private TypeElement[] findRuntimeAnnotations(RoundEnvironment roundEnvironment) {
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
                //See if the need to be processed in runtime
                .filter(e -> e.getAnnotation(NeedsRuntimeProcessing.class) != null)
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
            String modId = modLocator.forElement(e);
            if (modId == null) {
                continue;
            }

            nativeMetadata.getOrCreateModMetadata(modId).getAnnotatedData().addElement(e);
        }

        //Process user-defined runtime tasks
        for (Element e : roundEnv.getElementsAnnotatedWith(RegisteredRuntimeTask.class)) {
            RegisteredRuntimeTask taskAnnotation = e.getAnnotation(RegisteredRuntimeTask.class);
            Scope scope = taskAnnotation.value();
            String name = ((TypeElement) e).getQualifiedName().toString();
            String modId = modLocator.forElement(e);
            if (modId == null) {
                processingEnv.getMessager().printWarning("Got runtime task [" + name + "], but failed to compute the owning mod");
                continue;
            }

            nativeMetadata.getOrCreateModMetadata(modId).addRuntimeTask(scope, name);
        }

        return Set.of();
    }

    @Override
    public Set<Resource> finish(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        return Set.of(
                Resource.json(nativeMetadata)
                        .directory(NativeMetadata.DIRECTORY)
                        .name(NativeMetadata.FILE_ROOT)
                        .build()
        );
    }
}
