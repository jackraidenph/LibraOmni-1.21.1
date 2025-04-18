package dev.jackraidenph.libraomni.annotation.runtime;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.Registered;
import dev.jackraidenph.libraomni.util.StringUtilities;
import dev.jackraidenph.libraomni.util.context.ModContext;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class RegisteredAnnotationProcessor implements RuntimeProcessor {

    @Override
    public void process(ModContext modContext, Set<AnnotatedElement> elements) {
        for (AnnotatedElement annotatedElement : elements) {
            if (annotatedElement instanceof Class<?> clazz) {
                this.tryRegisterClass(modContext, clazz);
            }
        }
    }

    private <T> void tryRegisterClass(ModContext modContext, Class<T> clazz) {
        Registered registered = clazz.getAnnotation(Registered.class);

        if (registered == null) {
            return;
        }

        String id = registered.value();

        if (id == null || id.isBlank()) {
            id = StringUtilities.snakeCase(clazz.getSimpleName());
        }

        try {
            Constructor<T> emptyConstructor = clazz.getDeclaredConstructor();
            emptyConstructor.setAccessible(true);

            DeferredRegister<? super T> register = modContext.getRegister(clazz);
            LibraOmni.LOGGER.info("Found [{}] registry for [{}] with superclass [{}]",
                    register.getRegistryName(),
                    clazz.getSimpleName(),
                    clazz.getSuperclass().getSimpleName()
            );
            register.register(id, () -> safeConstruct(emptyConstructor));
            LibraOmni.LOGGER.info("Registered [{}:{}] to [{}]", modContext.modId(), id, register.getRegistryName());
        } catch (NoSuchMethodException noSuchMethodException) {
            LibraOmni.LOGGER.error("Failed to register [{}] as there's no proper empty constructor",
                    clazz.getSimpleName()
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
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(
                Registered.class
        );
    }
}
