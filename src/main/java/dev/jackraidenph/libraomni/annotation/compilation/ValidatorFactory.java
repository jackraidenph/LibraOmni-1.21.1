package dev.jackraidenph.libraomni.annotation.compilation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public enum ValidatorFactory {
    INSTANCE;

    private final Map<String, Validator> instances = new HashMap<>();

    ValidatorFactory() {
    }

    public Validator getOrCreate(String validatorClassName) {
        return instances.computeIfAbsent(validatorClassName, this::createIfNotPresent);
    }

    private Validator createIfNotPresent(String className) {
        Constructor<? extends Validator> emptyConstructor;
        try {
            Class<? extends Validator> validator = Class.forName(className).asSubclass(Validator.class);
            emptyConstructor = validator.getDeclaredConstructor();
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException("No default constructor found for " + className);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException("No such class exists: " + className);
        }

        try {
            return emptyConstructor.newInstance();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
