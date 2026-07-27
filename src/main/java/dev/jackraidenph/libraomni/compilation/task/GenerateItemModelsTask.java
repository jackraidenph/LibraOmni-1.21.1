package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.ArbitraryItemModelData;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

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

        processingContext.resourceManager().saveAndCache(
                ResourceIdentifier.builder()
                        .setAssetDirectory(modId, "models/item")
                        .setNameRoot(elementId)
                        .setJsonExtension()
                        .build(),
                modelMap,
                this.className()
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
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.compareWithClass(mirror, ArbitraryItemModelData.class);
    }
}
