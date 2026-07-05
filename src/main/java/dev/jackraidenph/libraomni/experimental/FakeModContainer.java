package dev.jackraidenph.libraomni.experimental;

import dev.jackraidenph.libraomni.LibraOmni;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.EventBusErrorMessage;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IConfigurable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;

public class FakeModContainer extends ModContainer {

    private final IEventBus eventBus;
    private final List<Class<?>> modClasses;

    public FakeModContainer(String modId, List<Class<?>> classes) {
        super(new MinimalModInfo(modId));
        eventBus = BusBuilder.builder()
                .setExceptionHandler(this::onEventFailed)
                .markerType(IModBusEvent.class)
                .allowPerPhasePost()
                .build();
        modClasses = classes;
    }

    private void onEventFailed(IEventBus iEventBus, Event event, EventListener[] iEventListeners, int i, Throwable throwable) {
        StringBuilder buffer = new StringBuilder();
        new EventBusErrorMessage(event, i, iEventListeners, throwable).formatTo(buffer);
        LibraOmni.LOGGER.error(buffer.toString());
    }

    /**
     * Taken from FMLModContainer
     */
    public void construct() {
        for (Class<?> modClass : modClasses) {
            try {
                Constructor<?>[] constructors = modClass.getConstructors();
                if (constructors.length != 1) {
                    throw new RuntimeException("Mod class " + modClass + " must have exactly 1 public constructor, found " + constructors.length);
                }
                Constructor<?> constructor = constructors[0];

                Map<Class<?>, Object> allowedConstructorArgs = new HashMap<>();

                allowedConstructorArgs.put(IEventBus.class, eventBus);
                allowedConstructorArgs.put(ModContainer.class, this);
                allowedConstructorArgs.put(FMLModContainer.class, null);
                allowedConstructorArgs.put(Dist.class, Dist.CLIENT);

                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] constructorArgs = new Object[parameterTypes.length];

                for (int i = 0; i < parameterTypes.length; i++) {
                    Object argInstance = allowedConstructorArgs.get(parameterTypes[i]);
                    constructorArgs[i] = argInstance;
                }

                constructor.newInstance(constructorArgs);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to construct a mod through fake mod contained of [%s]".formatted(modId), e);
            }
        }
    }

    @Override
    public @Nullable IEventBus getEventBus() {
        return eventBus;
    }

    public static class MinimalModInfo extends ModInfo {
        public MinimalModInfo(String modId) {
            super(null, new MinimalConfigurable(modId));
        }
    }

    public static class MinimalConfigurable implements IConfigurable {

        private final String modId;

        public MinimalConfigurable(String modId) {
            this.modId = modId;
        }

        @Override
        public <T> Optional<T> getConfigElement(String... key) {
            if (key != null && key.length == 1 && key[0].equals("modId")) {
                //noinspection unchecked
                return (Optional<T>) Optional.of(modId);
            }
            return Optional.empty();
        }

        @Override
        public List<? extends IConfigurable> getConfigList(String... key) {
            return List.of();
        }
    }
}
