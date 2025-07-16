package dev.jackraidenph.libraomni.reflect.extension;

import dev.jackraidenph.libraomni.reflect.LifecycleSetup;
import dev.jackraidenph.libraomni.reflect.ModContext;

public abstract class AbstractModContextExtension implements LifecycleSetup {
    private final ModContext modContext;

    public AbstractModContextExtension(ModContext modContext) {
        this.modContext = modContext;
    }

    public ModContext getContext() {
        return this.modContext;
    }
}
