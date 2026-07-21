package dev.jackraidenph.libraomni.runtime.task;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.runtime.ProxiedAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.ModContext;

import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class SequentialRuntimeTask implements RuntimeTask {

    @Override
    public void process(Set<ProxiedAnnotatedElement> elements, ModContext modContext) {
        for (ProxiedAnnotatedElement e : elements) {
            if (skipAnnotations() && e.proxiedElement() instanceof Annotation) {
                continue;
            }
            String id = SafeReflectionUtil.resolveObjectName(e.proxiedElement());
            if (requireId() && (id == null || id.isBlank())) {
                LibraOmni.LOGGER.warn(
                        " Task [{}] requires elements to either have @Id annotation present, or DeferredHolder to be populated. Not the case for [{}] ",
                        this.getClass().getSimpleName(), e.proxiedElement()
                );
                continue;
            }

            processElement(e, id, modContext);
        }
    }

    abstract void processElement(ProxiedAnnotatedElement element, String elementId, ModContext modContext);

    public boolean requireId() {
        return false;
    }

    public boolean skipAnnotations() {
        return true;
    }
}
