package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.TextureWithColorsSwapped;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.common.StringUtilities.NamespaceDirectoryFile;
import dev.jackraidenph.libraomni.common.ImageHelper;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier.ResourceBuilder;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenerateRecoloredTextureTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        TextureWithColorsSwapped annotation = element.getAnnotation(TextureWithColorsSwapped.class);
        if(annotation == null) {
            throw new IllegalStateException();
        }

        String parentTexture = annotation.originalTexture();
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

        String fileSuffix = annotation.newTexturesuffix();
        ResourceBuilder builder = ResourceIdentifier.builder()
                .setAssetDirectory(parts.namespace(), parts.directory())
                .setNameRoot(parts.file())
                .setPngExtension();

        ResourceIdentifier textureLocation = builder.build();
        ResourceBuilder saveLocationBuilder = builder.setNameRoot(elementId);
        if(fileSuffix != null && !fileSuffix.isBlank()) {
            builder.withSuffix(fileSuffix);
        }


        ImageHelper.transformAndSavePng(
                textureLocation,
                saveLocationBuilder.build(),
                image -> ImageHelper.remapColors(image, paletteSwap),
                processingContext
        );
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(TextureWithColorsSwapped.class);
    }
}
