package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.util.ElementUtil;

import javax.lang.model.element.Element;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A compilation task that processes elements one-by-one
 */
public abstract class SequentialCompilationTask implements CompilationTask {

    @Override
    public void processRound(ProcessingContext processingContext) {
        Set<? extends Element> elements = ElementUtil.getAllElements(processingContext.roundEnvironment()).stream()
                .filter(e -> e.getAnnotationMirrors().stream().anyMatch(this::isMirrorSupported))
                .collect(Collectors.toSet());
        processElements(elements, processingContext);
    }

    protected void processElements(Set<? extends Element> elements, ProcessingContext processingContext) {
        for (Element e : elements) {
            ModIdGetter modIdGetter = processingContext.modIdGetter();
            try {
                processElement(modIdGetter.getOriginModId(e), modIdGetter.getObjectName(e), e, processingContext);
            } catch (Exception ex) {
                throw new RuntimeException("Exception processing element [%s]".formatted(e), ex);
            }
        }
    }

    abstract void processElement(String modId, String elementId, Element element, ProcessingContext processingContext);
}
