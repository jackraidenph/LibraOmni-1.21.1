package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.Id;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A compilation task that can process elements one-by-one
 */
public abstract class SequentialCompilationTask implements CompilationTask {

    public Collection<InMemoryResource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWithAny(supportedAnnotations());
        return processElements(modLocator, elements, roundEnv, processingEnv);
    }

    public Collection<InMemoryResource> processElements(ModIdGetter modLocator, Set<? extends Element> elements, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        List<InMemoryResource> resources = new ArrayList<>();
        for (Element e : elements) {
            if (skipAnnotations() && isAnnotation(e)) {
                continue;
            }
            Id id = e.getAnnotation(Id.class);
            if (requireIdAnnotation() && id == null) {
                processingEnv.getMessager().printWarning(
                        "Annotations %s require @Id annotation to be present, but it was not found on element [%s], skipping!"
                                .formatted(supportedAnnotations().stream().map(a -> '@' + a.getSimpleName()).toList(), e.getSimpleName())
                );
                continue;
            }
            resources.addAll(processElement(modLocator.forElement(e), id, e, roundEnv, processingEnv));
        }
        return resources;
    }

    abstract Collection<InMemoryResource> processElement(String modId, @Nullable Id idAnnotation, Element element, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);

    public boolean requireIdAnnotation() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }

}
