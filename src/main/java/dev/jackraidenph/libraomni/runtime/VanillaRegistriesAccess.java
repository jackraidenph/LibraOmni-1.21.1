package dev.jackraidenph.libraomni.runtime;

import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.UnsafeReflectionUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class VanillaRegistriesAccess {

    private static final Class<?> REGISTRIES_CLASS = Registries.class;
    private static final Map<Class<?>, ResourceKey<? extends Registry<?>>> VANILLA_REGISTRIES_MAPPING = new HashMap<>();

    public static <T> Entry<Class<T>, ResourceKey<Registry<T>>> getRegistryResourceKey(Class<T> clazz) {
        Class<T> supertype = SafeReflectionUtil.tryFindSuperclass(VANILLA_REGISTRIES_MAPPING.keySet(), clazz);
        if (supertype == null) {
            return null;
        }

        //Cast is meant to make sense by design of the collection
        //noinspection unchecked
        return Map.entry(supertype, (ResourceKey<Registry<T>>) VANILLA_REGISTRIES_MAPPING.get(supertype));
    }

    @SuppressWarnings("unchecked") //Only-internal binding
    private static Entry<Class<?>, ResourceKey<Registry<?>>> resolveResourceKeyField(Field key) {
        ParameterizedType genericType = (ParameterizedType) key.getGenericType();
        ParameterizedType registryType = (ParameterizedType) genericType.getActualTypeArguments()[0];
        Type resolvedType = registryType.getActualTypeArguments()[0];
        Class<?> clazz;
        if (!(resolvedType instanceof ParameterizedType)) {
            clazz = (Class<?>) resolvedType;
        } else {
            return null;
        }

        Object obj = UnsafeReflectionUtil.getFieldValue(key, null, false);
        if (obj == null) {
            throw new IllegalStateException("Failed to get %s value".formatted(key.toGenericString()));
        }

        ResourceKey<Registry<?>> resourceKey;
        try {
            resourceKey = (ResourceKey<Registry<?>>) obj;
        } catch (ClassCastException castException) {
            throw new IllegalArgumentException(castException);
        }

        return Map.entry(clazz, resourceKey);
    }

    public static void mapAndCacheVanillaRegistries() {
        VANILLA_REGISTRIES_MAPPING.clear();
        Arrays.stream(REGISTRIES_CLASS.getDeclaredFields())
                //public static
                .filter(f -> f.accessFlags().containsAll(Set.of(AccessFlag.PUBLIC, AccessFlag.STATIC)))
                //ResourceKeys (ResourceKey<Registry<Block>> BLOCK = createRegistryKey("block"))
                .filter(f -> f.getType().equals(ResourceKey.class))
                //map to Class -> ResourceKey pairs
                .map(VanillaRegistriesAccess::resolveResourceKeyField)
                //filter out nulls (if parametrized)
                .filter(Objects::nonNull)
                //cache
                .forEach(e -> VANILLA_REGISTRIES_MAPPING.put(e.getKey(), e.getValue()));
    }
}
