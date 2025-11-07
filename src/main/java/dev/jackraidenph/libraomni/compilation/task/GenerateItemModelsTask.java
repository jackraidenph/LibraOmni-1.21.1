package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.value.KeyValue;
import dev.jackraidenph.libraomni.annotation.datagen.GeneratesItemModelData;
import dev.jackraidenph.libraomni.compilation.util.*;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

class GenerateItemModelsTask extends SequentialCompilationTask {

    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        GeneratesItemModelData annotation = element.getAnnotation(GeneratesItemModelData.class);
        Map<String, String> textures = mapTextures(annotation);

        Map<String, Object> modelMap = new HashMap<>();

        String parent = annotation.parentModel();
        if (parent == null || parent.isBlank()) {
            parent = modId + ":block/" + elementId;
        }

        modelMap.put("parent", parent);
        if (!textures.isEmpty()) {
            modelMap.put("textures", textures);
        }

        processingContext.resourceManager().save(
                ResourceIdentifier.builder()
                        .setAssetDirectory(modId, "models/item")
                        .setNameRoot(elementId)
                        .setJsonExtension()
                        .build(),
                modelMap
        );
    }

    private Map<String, String> mapTextures(GeneratesItemModelData annotation) {
        Map<String, String> textures = new HashMap<>();
        for (KeyValue keyValue : annotation.value()) {
            String textureName = keyValue.value();
            if (textureName.isBlank()) {
                continue;
            }

            textures.put(keyValue.key(), textureName);
        }

        return textures;
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public final Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(GeneratesItemModelData.class);
    }
}
