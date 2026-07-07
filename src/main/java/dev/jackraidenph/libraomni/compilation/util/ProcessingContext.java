package dev.jackraidenph.libraomni.compilation.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

public record ProcessingContext(ResourceManager resourceManager,
                                AnnotationProcessorConfig config,
                                RoundEnvironment roundEnvironment,
                                ProcessingEnvironment processingEnvironment
) {
}
