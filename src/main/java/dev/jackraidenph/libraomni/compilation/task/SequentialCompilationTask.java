package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.Id;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A compilation task that can process elements one-by-one
 */
public abstract class SequentialCompilationTask implements CompilationTask {

    public void processRound(ModIdGetter modLocator, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWithAny(supportedAnnotations());
        processElements(modLocator, elements, resourceManager, roundEnv, processingEnv);
    }

    protected void processElements(ModIdGetter modLocator, Set<? extends Element> elements, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
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
            processElement(modLocator.forElement(e), getId(e), e, resourceManager, roundEnv, processingEnv);
        }
    }

    private static String getId(Element e) {
        Id id = e.getAnnotation(Id.class);
        if (id != null && !id.value().isBlank()) {
            return id.value();
        }

        return StringUtilities.snakeCase(e.getSimpleName().toString());
    }

    abstract void processElement(String modId, String elementId, Element element, ResourceManager resourceManager, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);

    public boolean requireIdAnnotation() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }

}
