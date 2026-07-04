package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.GeneratesPalettedTexture;
import dev.jackraidenph.libraomni.common.ImageHelper;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.StringUtilities.NamespaceDirectoryFile;
import dev.jackraidenph.libraomni.compilation.util.*;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public class GeneratePalettedTextureTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        GeneratesPalettedTexture annotation = element.getAnnotation(GeneratesPalettedTexture.class);
        if(annotation == null) {
            throw new IllegalStateException();
        }

        String parentTexture = annotation.parentTexture();

        NamespaceDirectoryFile parts = StringUtilities.splitToNamespaceDirFilename(
                parentTexture,
                "minecraft",
                "textures",
                elementId
        );

        String fileSuffix = annotation.suffix();
        ImageHelper.transformAndSavePng(
                parts.namespace(),
                parts.directory(),
                parts.file(),
                fileSuffix.isBlank() ? null : ResourceIdentifier.pngAsset(parts.namespace(), parts.directory(), parts.file() + "_" + fileSuffix),
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
        return Set.of(GeneratesPalettedTexture.class);
    }
}
