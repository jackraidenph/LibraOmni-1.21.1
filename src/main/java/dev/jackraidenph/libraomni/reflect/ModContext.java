package dev.jackraidenph.libraomni.reflect;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.HashMap;
import java.util.Map;

public class ModContext implements LifecycleSetup {

    private final ModContainer modContainer;

    private final Map<Class<? extends AbstractModContextExtension>, AbstractModContextExtension> extensions = new HashMap<>();

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    void registerExtension(AbstractModContextExtension extension) {
        Class<? extends AbstractModContextExtension> clazz = extension.getClass();
        if (extensions.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        this.extensions.put(clazz, extension);
    }

    public String modId() {
        return this.modContainer.getModId();
    }

    public <T extends AbstractModContextExtension> T getExtension(Class<T> extensionClass) {
        //noinspection unchecked
        return (T) this.extensions.get(extensionClass);
    }

    @Override
    public void listenToBus(IEventBus eventBus) {
        for (AbstractModContextExtension extension : extensions.values()) {
            extension.listenToBus(eventBus);
        }
    }

    public ModContainer modContainer() {
        return modContainer;
    }
}
