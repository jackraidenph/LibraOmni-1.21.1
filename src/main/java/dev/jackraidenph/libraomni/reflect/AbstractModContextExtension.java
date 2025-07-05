package dev.jackraidenph.libraomni.reflect;

public abstract class AbstractModContextExtension implements LifecycleSetup{
    private final ModContext modContext;

    public AbstractModContextExtension(ModContext modContext) {
        this.modContext = modContext;
    }

    public ModContext getContext() {
        return this.modContext;
    }
}
