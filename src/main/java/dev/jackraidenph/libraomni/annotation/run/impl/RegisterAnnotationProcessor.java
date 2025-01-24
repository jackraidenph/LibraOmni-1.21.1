package dev.jackraidenph.libraomni.annotation.run.impl;

import dev.jackraidenph.libraomni.annotation.impl.Register;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.util.ReferenceMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;
import net.minecraft.world.level.block.Block;

import java.lang.annotation.Annotation;
import java.util.Locale;

public class RegisterAnnotationProcessor implements RuntimeProcessor {

    @Override
    public void process(
            ModContext modContext,
            Class<? extends Annotation> annotation,
            AnnotatedElement<?> annotatedElement
    ) {
        if (annotatedElement.isSubclassOf(Block.class)) {
            Class<Block> blockClass = (Class<Block>) annotatedElement.element();

            Register register = (Register) blockClass.getAnnotation(annotation);

            String id = register.value();

            if (id == null || id.isBlank()) {
                id = blockClass.getSimpleName().toLowerCase(Locale.ROOT);
            }

            modContext.getRegisterHandler().blocksRegister().registerBlock(
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
        return Register.class;
    }
}
