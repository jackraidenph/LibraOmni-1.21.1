package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.Set;

/**
 * A compilation task that can process elements one-by-one
 */
public abstract class SequentialCompilationTask implements CompilationTask {

    @Override
    public void processRound(ProcessingContext processingContext) {
        Set<? extends Element> elements = processingContext.roundEnvironment().getElementsAnnotatedWithAny(supportedAnnotations());
        processElements(elements, processingContext);
    }

    protected void processElements(Set<? extends Element> elements, ProcessingContext processingContext) {
        for (Element e : elements) {
            if (skipAnnotations() && e.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
                continue;
            }
//            Id id = e.getAnnotation(Id.class);
//            if (requireIdAnnotation() && id == null) {
//                processingContext.processingEnvironment().getMessager().printWarning(
//                        "Annotations %s require @Id annotation to be present, but it was not found on element [%s], skipping!"
//                                .formatted(supportedAnnotations().stream().map(a -> '@' + a.getSimpleName()).toList(), e.getSimpleName())
//                );
//                continue;
//            }
            processElement(processingContext.modIdGetter().modIdByElement(e), ModIdGetter.getElementId(e), e, processingContext);
        }
    }

    abstract void processElement(String modId, String elementId, Element element, ProcessingContext processingContext);

    public boolean requireIdAnnotation() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }

}
