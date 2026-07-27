package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.ArbitraryBlockModelData;
import dev.jackraidenph.libraomni.annotation.value.StringPair;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

class GenerateBlockModelsTask extends SequentialCompilationTask {

    @Override
    public void processElement(String modId, @Nullable String elementId, Element element, ProcessingContext processingContext) {
        ArbitraryBlockModelData[] annotations = element.getAnnotationsByType(ArbitraryBlockModelData.class);
        for (ArbitraryBlockModelData annotation : annotations) {
            Map<String, String> textures = mapTextures(modId, elementId, annotation);
            processingContext.resourceManager().saveAndCache(
                    ResourceIdentifier.jsonAsset(modId, "models/block", elementId),
                    Map.of("parent", annotation.parentModel(), "textures", textures),
                    this.className()
            );
        }
    }

    private Map<String, String> mapTextures(String modId, String elementId, ArbitraryBlockModelData annotation) {
        Map<String, String> textures = new HashMap<>();
        for (StringPair keyValue : annotation.value()) {
            String textureName = keyValue.value();
            if (textureName == null || textureName.isBlank()) {
                continue;
            }

            textures.put(keyValue.key(), textureName);
        }

        if (textures.isEmpty()) {
            return Map.of("all", StringUtil.makeNamespacedId(modId, elementId));
        }

        return textures;
    }

    @Override
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.compareWithClass(mirror, ArbitraryBlockModelData.class);
    }
}
