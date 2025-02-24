package dev.jackraidenph.libraomni.annotation.run.impl;

import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;
import net.minecraft.world.level.block.Block;

import java.lang.annotation.Annotation;
import java.util.Locale;

public class RegisteredAnnotationProcessor implements RuntimeProcessor {

    public static RegisteredAnnotationProcessor INSTANCE = new RegisteredAnnotationProcessor();

    private RegisteredAnnotationProcessor() {

    }

    @Override
    public void process(
            ModContext modContext,
            Class<? extends Annotation> annotation,
            AnnotatedElement<?> annotatedElement
    ) {
        if (annotatedElement.isSubclassOf(Block.class)) {
            //Suppress, because we actually check the case
            //noinspection unchecked
            Class<Block> blockClass = (Class<Block>) annotatedElement.element();

            Registered register = (Registered) blockClass.getAnnotation(annotation);

            String id = register.value();

            if (id == null || id.isBlank()) {
                id = blockClass.getSimpleName().toLowerCase(Locale.ROOT);
            }

            modContext.blocksRegister().registerBlock(
                    id,
                    Block::new
            );
        }
    }

    @Override
    public Scope getScope() {
        return Scope.CONSTRUCT;
    }

    @Override
    public Class<? extends Annotation> getSupportedAnnotation() {
        return Registered.class;
    }
}
