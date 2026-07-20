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

            ModIdGetter modIdGetter = processingContext.modIdGetter();
            try {
                processElement(modIdGetter.getOriginModId(e), modIdGetter.getObjectName(e), e, processingContext);
            } catch (Exception ex) {
                throw new RuntimeException("Exception processing element [%s]".formatted(e), ex);
            }
        }
    }

    abstract void processElement(String modId, String elementId, Element element, ProcessingContext processingContext);

    public boolean skipAnnotations() {
        return true;
    }

}
