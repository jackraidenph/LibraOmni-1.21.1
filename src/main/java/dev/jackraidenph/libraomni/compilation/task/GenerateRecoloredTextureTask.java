package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.GeneratesRecoloredTexture;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.StringUtilities.NamespaceDirectoryFile;
import dev.jackraidenph.libraomni.common.ImageHelper;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenerateRecoloredTextureTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        GeneratesRecoloredTexture annotation = element.getAnnotation(GeneratesRecoloredTexture.class);
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

        Map<Integer, Integer> paletteSwap = new HashMap<>();
        if (annotation.oldColors().length != annotation.newColors().length) {
            String name = annotation.annotationType().getSimpleName();
            throw new IllegalArgumentException("%s#newColors() must be the same size as %s#oldColors()".formatted(name, name));
        }
        for (int i = 0; i < annotation.oldColors().length; i++) {
            paletteSwap.put(annotation.oldColors()[i], annotation.newColors()[i]);
        }

        String fileSuffix = annotation.suffix();
        ImageHelper.transformAndSavePng(
                parts.namespace(),
                parts.directory(),
                parts.file(),
                fileSuffix.isBlank() ? null : ResourceIdentifier.pngAsset(parts.namespace(), parts.directory(), parts.file() + "_" + fileSuffix),
                image -> ImageHelper.remapColors(image, paletteSwap),
                processingContext
        );
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(GeneratesRecoloredTexture.class);
    }
}
