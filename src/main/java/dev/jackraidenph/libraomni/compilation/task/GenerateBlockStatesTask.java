package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.BlockStateModel;
import dev.jackraidenph.libraomni.common.StringUtil;
import dev.jackraidenph.libraomni.compilation.util.*;
import net.minecraft.resources.ResourceLocation;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

class GenerateBlockStatesTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        BlockStateModel generatesBlockStateModelData = element.getAnnotation(BlockStateModel.class);
        if (generatesBlockStateModelData == null) {
            throw new IllegalStateException();
        }

        String model = generatesBlockStateModelData.model();
        ResourceLocation resourceLocation = ResourceLocation.tryParse(model);
        if (resourceLocation == null) {
            throw new IllegalStateException("[%s] is not a valid resource location!".formatted(model));
        }

        processingContext.resourceManager().save(
                defaultBlockState(elementId, modId, resourceLocation.getNamespace(), resourceLocation.getPath())
        );
    }

    private static InMemoryResource defaultBlockState(String fileName, String modId, String modelNamespace, String modelName) {

        var json = Map.of(
                "variants", Map.of(
                        "", Map.of(
                                "model", StringUtil.makeNamespacedId(modelNamespace, modId, modelName)
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
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(BlockStateModel.class);
    }
}
