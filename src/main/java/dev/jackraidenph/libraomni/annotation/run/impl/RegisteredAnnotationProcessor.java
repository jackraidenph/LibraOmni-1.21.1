package dev.jackraidenph.libraomni.annotation.run.impl;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.util.ModContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
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
            id = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        }

        try {
            Constructor<T> emptyConstructor = clazz.getDeclaredConstructor();
            emptyConstructor.setAccessible(true);

            if (Block.class.isAssignableFrom(clazz)) {
                this.registerBlock(modContext.blocksRegister(), id, emptyConstructor);
            } else if (Item.class.isAssignableFrom(clazz)) {
                this.registerItem(modContext.itemsRegister(), id, emptyConstructor);
            } else {
                DeferredRegister<T> register = modContext.getRegister(clazz);
                register.register(id, () -> safeConstruct(emptyConstructor));
            }
        } catch (NoSuchMethodException noSuchMethodException) {
            LibraOmni.LOGGER.error("Failed to register [{}] as there's no proper empty constructor",
                    clazz.getSimpleName()
            );
        }
    }

    private void registerBlock(DeferredRegister.Blocks register, String id, Constructor<?> constructor) {
        register.register(id, () -> (Block) safeConstruct(constructor));
    }

    private void registerItem(DeferredRegister.Items register, String id, Constructor<?> constructor) {
        register.register(id, () -> (Item) safeConstruct(constructor));
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
    public Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(
                Registered.class
        );
    }
}
