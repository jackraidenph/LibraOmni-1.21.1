package dev.jackraidenph.libraomni.annotation.run;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.util.ReferenceMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;
import dev.jackraidenph.libraomni.annotation.run.util.ReferenceMapReader;
import dev.jackraidenph.libraomni.annotation.run.util.ReferenceMapReader.ElementStorage;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class RuntimeProcessorsManager {

    private final ModContext modContext;
    private final List<RuntimeProcessor> runTimeProcessors = new ArrayList<>();

    private final ElementStorage elementStorage;

    public RuntimeProcessorsManager(ModContext modContext) {
        this.modContext = modContext;
        String modId = modContext.modContainer().getModId();

        ReferenceMapReader referenceMapReader = new ReferenceMapReader(
                modId,
                LibraOmni.Utility.markedLocationForMod(modId)
        );

        this.elementStorage = referenceMapReader.readElements();
    }

    public void registerProcessor(RuntimeProcessor runTimeProcessor) {
        if (runTimeProcessors.contains(runTimeProcessor)) {
            throw new IllegalArgumentException("Runtime processor already registered");
        }
        this.runTimeProcessors.add(runTimeProcessor);
    }

    public void onProcess(Scope scope) {
        for (RuntimeProcessor runtimeProcessor : this.runTimeProcessors) {
            for (Class<? extends Annotation> annotation : this.elementStorage.getAnnotations()) {
                if (runtimeProcessor.getSupportedAnnotations().contains(annotation)) {
                    for (AnnotatedElement<?> annotatedElement : this.elementStorage.getElements(annotation)) {
                        LibraOmni.LOGGER.info(
                                "Processing [{}] element with {} for {} annotation with scope {}",
                                annotatedElement.element().toString(),
                                runtimeProcessor.getClass().getSimpleName(),
                                annotation.getSimpleName(),
                                scope.name()
                        );
                        runtimeProcessor.process(this.modContext, scope, annotation, annotatedElement);
                    }
                }
            }
        }
    }

    public void onFinish() {
        for (RuntimeProcessor runtimeProcessor : this.runTimeProcessors) {
            LibraOmni.LOGGER.info(
                    "Finishing {}",
                    runtimeProcessor.getClass().getSimpleName()
            );
            runtimeProcessor.onFinish(this.modContext);
        }
    }
}
