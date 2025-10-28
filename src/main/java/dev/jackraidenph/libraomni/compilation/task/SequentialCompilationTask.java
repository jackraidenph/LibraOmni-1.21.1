package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.Id;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.lang.model.element.Element;
import java.util.Set;

/**
 * A compilation task that can process elements one-by-one
 */
public abstract class SequentialCompilationTask implements CompilationTask {

    public void processRound(ModIdGetter modLocator, ProcessingContext processingContext) {
        System.out.println(supportedAnnotations());
        Set<? extends Element> elements = processingContext.roundEnvironment().getElementsAnnotatedWithAny(supportedAnnotations());
        System.out.println(elements);
        System.out.println();
        processElements(modLocator, elements, processingContext);
    }

    protected void processElements(ModIdGetter modLocator, Set<? extends Element> elements, ProcessingContext processingContext) {
        for (Element e : elements) {
            if (skipAnnotations() && isAnnotation(e)) {
                continue;
            }
            Id id = e.getAnnotation(Id.class);
            if (requireIdAnnotation() && id == null) {
                processingContext.processingEnvironment().getMessager().printWarning(
                        "Annotations %s require @Id annotation to be present, but it was not found on element [%s], skipping!"
                                .formatted(supportedAnnotations().stream().map(a -> '@' + a.getSimpleName()).toList(), e.getSimpleName())
                );
                continue;
            }
            processElement(modLocator.forElement(e), getId(e), e, processingContext);
        }
    }

    private static String getId(Element e) {
        Id id = e.getAnnotation(Id.class);
        if (id != null && !id.value().isBlank()) {
            return id.value();
        }

        return StringUtilities.snakeCase(e.getSimpleName().toString());
    }

    abstract void processElement(String modId, String elementId, Element element, ProcessingContext processingContext);

    public boolean requireIdAnnotation() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }

}
