package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.ProxyAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.ModContext;

import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class SequentialRuntimeTask implements RuntimeTask {

    @Override
    public void process(Set<ProxyAnnotatedElement> elements, ModContext modContext) {
        for (ProxyAnnotatedElement e : elements) {
            if (skipAnnotations() && e.original() instanceof Annotation) {
                continue;
            }
            String id = SafeReflectionUtil.id(e);
            if (requireId() && (id == null || id.isBlank())) {
                LibraOmni.LOGGER.warn(
                        " Task [{}] requires elements to either have @Id annotation present, or DeferredHolder to be populated. Not the case for [{}] ",
                        this.getClass().getSimpleName(), e.original()
                );
                continue;
            }

            processElement(e, id, modContext);
        }
    }

    abstract void processElement(ProxyAnnotatedElement element, String elementId, ModContext modContext);

    public boolean requireId() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }
}
