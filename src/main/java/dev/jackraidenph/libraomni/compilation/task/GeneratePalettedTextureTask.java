package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.TextureWithPalete;
import dev.jackraidenph.libraomni.util.ImageHelper;
import dev.jackraidenph.libraomni.util.StringUtil;
import dev.jackraidenph.libraomni.util.StringUtil.NamespaceDirectoryFile;
import dev.jackraidenph.libraomni.compilation.util.*;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier.ResourceBuilder;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public class GeneratePalettedTextureTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        TextureWithPalete annotation = element.getAnnotation(TextureWithPalete.class);
        if (annotation == null) {
            throw new IllegalStateException();
        }

        String parentTexture = annotation.originalTexture();

        NamespaceDirectoryFile parts = StringUtil.splitToNamespaceDirFilename(parentTexture, "minecraft", "textures");

        ResourceBuilder builder = ResourceIdentifier.builder()
                .setAssetDirectory(parts.namespace(), parts.directory())
                .setNameRoot(parts.file())
                .setPngExtension();

        ResourceIdentifier textureLocation = builder.build();
        ResourceIdentifier saveLocation = builder.setNameRoot(elementId).build();

        InMemoryResource newPng = ImageHelper.transformPng(
                textureLocation,
                saveLocation,
                image -> ImageHelper.recolor(image, annotation.palette(), annotation.usePaletteInterpolation()),
                processingContext
        );

        processingContext.resourceManager().saveAndCache(newPng, this.className());
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(TextureWithPalete.class);
    }
}
