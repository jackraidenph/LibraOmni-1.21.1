package dev.jackraidenph.libraomni.context.extension.base;

import dev.jackraidenph.libraomni.context.ModContext;

public abstract class AbstractModContextExtension implements ModContextExtension {

    private final ModContext modContext;

    public AbstractModContextExtension(ModContext modContext) {
        this.modContext = modContext;
    }

    public ModContext getModContext() {
        return modContext;
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onCommonSetup() {
    }

    @Override
    public void onClientSetup() {
    }

    @Override
    public void onModConstruct() {
    }
}
