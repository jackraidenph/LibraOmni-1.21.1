package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.value.KeyValue;
import dev.jackraidenph.libraomni.annotation.datagen.GeneratesBlockModelData;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

class GenerateBlockModelsTask extends SequentialCompilationTask {

    @Override
    public void processElement(String modId, @Nullable String elementId, Element element, ProcessingContext processingContext) {
        GeneratesBlockModelData[] annotations = element.getAnnotationsByType(GeneratesBlockModelData.class);
        for (GeneratesBlockModelData annotation : annotations) {
            Map<String, String> textures = mapTextures(modId, elementId, annotation);
            processingContext.resourceManager().save(
                    ResourceIdentifier.jsonAsset(modId, "models/block", elementId),
                    Map.of("parent", annotation.parentModel(), "textures", textures)
            );
        }
    }

    private Map<String, String> mapTextures(String modId, String elementId, GeneratesBlockModelData annotation) {
        Map<String, String> textures = new HashMap<>();
        for (KeyValue keyValue : annotation.value()) {
            String textureName = keyValue.value();
            if (textureName == null || textureName.isBlank()) {
                continue;
            }

            textures.put(keyValue.key(), textureName);
        }

        if (textures.isEmpty()) {
            return Map.of("all", StringUtilities.makeNamespacedId(modId, elementId));
        }

        return textures;
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public final Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(GeneratesBlockModelData.class);
    }
}
