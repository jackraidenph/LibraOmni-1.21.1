package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.annotation.datagen.ArbitraryItemModelData;
import dev.jackraidenph.libraomni.compilation.util.*;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

class GenerateItemModelsTask extends SequentialCompilationTask {

    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        ArbitraryItemModelData annotation = element.getAnnotation(ArbitraryItemModelData.class);
        if (annotation == null) {
            throw new IllegalStateException();
        }

        Map<String, String> textures = mapTextures(annotation);

        Map<String, Object> modelMap = new HashMap<>();

        String parent = annotation.parentModel();

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

    private Map<String, String> mapTextures(ArbitraryItemModelData annotation) {
        Map<String, String> textures = new HashMap<>();
        for (StringPair keyValue : annotation.value()) {
            String textureName = keyValue.value();
            if (textureName.isBlank()) {
                continue;
            }

            textures.put(keyValue.key(), textureName);
        }

        return textures;
    }

    @Override
    public final Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(ArbitraryItemModelData.class);
    }
}
