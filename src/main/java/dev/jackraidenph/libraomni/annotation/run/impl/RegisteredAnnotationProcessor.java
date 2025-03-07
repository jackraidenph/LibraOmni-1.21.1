package dev.jackraidenph.libraomni.annotation.run.impl;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.util.AnnotationMapReader.ElementStorage.AnnotatedElement;
import dev.jackraidenph.libraomni.context.ModContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

public class RegisteredAnnotationProcessor implements RuntimeProcessor<Registered> {

    public static RegisteredAnnotationProcessor INSTANCE = new RegisteredAnnotationProcessor();

    private RegisteredAnnotationProcessor() {

    }

    @Override
    public void process(
            ModContext modContext,
            AnnotatedElement<?> annotatedElement
    ) {
        Registered registered = this.getElementAnnotation(annotatedElement);

        if (registered == null) {
            return;
        }

        String id = registered.value();

        if (id == null || id.isBlank()) {
            id = annotatedElement.asClass().getSimpleName().toLowerCase(Locale.ROOT);
        }

        try {
            Constructor<?> emptyConstructor = annotatedElement.asClass().getConstructor();

            modContext.blocksRegister().register(
                    id,
                    () -> (Block) safeConstruct(emptyConstructor)
            );
        } catch (NoSuchMethodException noSuchMethodException) {
            LibraOmni.LOGGER.error("Failed to register [{}] as there's no proper empty constructor",
                    annotatedElement.asClass().getSimpleName()
            );
        }
    }

    private static <E> E safeConstruct(Constructor<E> emptyConstructor) {
        if (emptyConstructor.getParameterCount() > 0) {
            throw new IllegalArgumentException("The constructor must be empty");
        }

        try {
            return emptyConstructor.newInstance();
        } catch (InvocationTargetException e) {
            throw new RuntimeException("There was an exception inside the empty constructor", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to instantiate the class, InstantiationException was thrown. Check that your class is not abstract or interface");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get access to the empty constructor");
        }
    }

    @Override
    public Scope getScope() {
        return Scope.CONSTRUCT;
    }

    @Override
    public Class<Registered> getSupportedAnnotation() {
        return Registered.class;
    }
}
