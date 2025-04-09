package dev.jackraidenph.libraomni.annotation.compilation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

enum ValidatorFactory {
    INSTANCE;

    ValidatorFactory() {
    }

    public Validator create(String className) {
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
