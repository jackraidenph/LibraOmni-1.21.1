package dev.jackraidenph.libraomni.annotation.run;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.impl.resource.AnnotationMapProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader.ElementStorage;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class RuntimeProcessorsManager {

    private final ModContext modContext;
    private final List<RuntimeProcessor<?>> runTimeProcessors = new ArrayList<>();

    private final ElementStorage elementStorage;

    public RuntimeProcessorsManager(ModContext modContext) {
        this.modContext = modContext;
        String modId = modContext.modContainer().getModId();

        AnnotationMapReader annotationMapReader = new AnnotationMapReader(
                modId,
                AnnotationMapProcessor.annotationsForModId(modId)
        );

        this.elementStorage = annotationMapReader.readElements();
    }

    public void registerProcessor(RuntimeProcessor<?> runTimeProcessor) {
        if (runTimeProcessors.contains(runTimeProcessor)) {
            throw new IllegalArgumentException("Runtime processor already registered");
        }
        this.runTimeProcessors.add(runTimeProcessor);
    }

    public void onProcess(Scope scope) {
        for (RuntimeProcessor<?> runtimeProcessor : this.runTimeProcessors) {
            if (runtimeProcessor.getScope().equals(scope)) {
                this.processForScope(runtimeProcessor);
            }
        }
    }

    private void processForScope(RuntimeProcessor<?> runtimeProcessor) {
        for (Class<? extends Annotation> annotation : this.elementStorage.getAnnotations()) {
            if (runtimeProcessor.getSupportedAnnotation().isAssignableFrom(annotation)) {
                this.processAnnotation(runtimeProcessor, annotation);
            }
        }
    }

    private void processAnnotation(RuntimeProcessor<?> runtimeProcessor, Class<? extends Annotation> annotation) {
        for (AnnotatedElement<?> annotatedElement : this.elementStorage.getElements(annotation)) {
            LibraOmni.LOGGER.info(
                    "Processing [{}] with {} for {} annotation with scope {}",
                    annotatedElement.element().toString(),
                    runtimeProcessor.getClass().getSimpleName(),
                    annotation.getSimpleName(),
                    runtimeProcessor.getScope().name()
            );

            runtimeProcessor.process(this.modContext, annotatedElement);
        }
    }
}
