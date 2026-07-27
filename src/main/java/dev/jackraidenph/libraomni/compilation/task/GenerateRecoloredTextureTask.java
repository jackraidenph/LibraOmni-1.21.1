package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.TextureWithColorsSwapped;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier.ResourceBuilder;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ImageHelper;
import dev.jackraidenph.libraomni.util.StringUtil;
import dev.jackraidenph.libraomni.util.StringUtil.NamespaceDirectoryFile;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

public class GenerateRecoloredTextureTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        TextureWithColorsSwapped annotation = element.getAnnotation(TextureWithColorsSwapped.class);
        if (annotation == null) {
            throw new IllegalStateException();
        }

        String parentTexture = annotation.originalTexture();
        NamespaceDirectoryFile parts = StringUtil.splitToNamespaceDirFilename(parentTexture, "minecraft", "textures");

        Map<Integer, Integer> paletteSwap = new HashMap<>();
        if (annotation.oldColors().length != annotation.newColors().length) {
            String name = annotation.annotationType().getSimpleName();
            throw new IllegalArgumentException("%s#newColors() must be the same size as %s#oldColors()".formatted(name, name));
        }
        for (int i = 0; i < annotation.oldColors().length; i++) {
            paletteSwap.put(annotation.oldColors()[i], annotation.newColors()[i]);
        }

        ResourceBuilder builder = ResourceIdentifier.builder()
                .setAssetDirectory(parts.namespace(), parts.directory())
                .setNameRoot(parts.file())
                .setPngExtension();

        ResourceIdentifier textureLocation = builder.build();
        ResourceIdentifier saveLocation = builder.setNameRoot(elementId).build();

        InMemoryResource newPng = ImageHelper.transformPng(
                textureLocation,
                saveLocation,
                image -> ImageHelper.remapColors(image, paletteSwap),
                processingContext
        );

        processingContext.resourceManager().saveAndCache(newPng, this.className());
    }

    @Override
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.compareWithClass(mirror, TextureWithColorsSwapped.class);
    }
}
