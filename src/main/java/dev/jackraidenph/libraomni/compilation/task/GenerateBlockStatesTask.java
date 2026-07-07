package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.BlockStateModelData;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.*;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

class GenerateBlockStatesTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        BlockStateModelData generatesBlockStateModelData = element.getAnnotation(BlockStateModelData.class);

        String model = generatesBlockStateModelData.model();
        int separator = model.indexOf(':');
        String modelNamespace = null;
        String modelName;
        if (!model.isBlank()) {
            if (separator > 0) {
                String[] parts = model.split(":");
                modelNamespace = parts[0];
                modelName = parts[1];
            } else {
                modelName = model;
            }
        } else {
            modelNamespace = modId;
            modelName = elementId;
        }

        processingContext.resourceManager().save(
                defaultBlockState(elementId, modId, modelNamespace, modelName)
        );
    }

    private static InMemoryResource defaultBlockState(String fileName, String modId, String modelNamespace, String modelName) {

        var json = Map.of(
                "variants", Map.of(
                        "", Map.of(
                                "model", StringUtilities.makeNamespacedId(modelNamespace, modId, "block/" + modelName)
                        )
                )
        );

        return new InMemoryResource(
                ResourceIdentifier.builder()
                        .setAssetDirectory(modId, "blockstates")
                        .setNameRoot(fileName)
                        .setJsonExtension()
                        .build(),
                json
        );
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(BlockStateModelData.class);
    }
}
