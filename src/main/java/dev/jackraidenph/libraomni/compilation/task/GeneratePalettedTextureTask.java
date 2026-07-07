package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.TextureWithPalete;
import dev.jackraidenph.libraomni.common.ImageHelper;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.StringUtilities.NamespaceDirectoryFile;
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

        NamespaceDirectoryFile parts = StringUtilities.splitToNamespaceDirFilename(
                parentTexture,
                "minecraft",
                "textures",
                elementId
        );

        String fileSuffix = annotation.newTexturesuffix();
        ResourceBuilder builder = ResourceIdentifier.builder()
                .setAssetDirectory(parts.namespace(), parts.directory())
                .setNameRoot(parts.file())
                .setPngExtension();

        ResourceIdentifier textureLocation = builder.build();
        ResourceIdentifier saveOverride = fileSuffix.isBlank() ? null : builder.withSuffix(fileSuffix).build();

        ImageHelper.transformAndSavePng(
                textureLocation,
                saveOverride,
                image -> ImageHelper.recolor(image, annotation.palette(), annotation.usePaletteInterpolation()),
                processingContext
        );
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(TextureWithPalete.class);
    }
}
