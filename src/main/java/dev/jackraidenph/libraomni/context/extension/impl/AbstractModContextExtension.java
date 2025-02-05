package dev.jackraidenph.libraomni.context.extension.impl;

import dev.jackraidenph.libraomni.context.ModContext;
import dev.jackraidenph.libraomni.context.extension.api.ModContextExtension;

public abstract class AbstractModContextExtension implements ModContextExtension {

    private final ModContext modContext;

    public AbstractModContextExtension(ModContext modContext) {
        this.modContext = modContext;
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

    @Override
    public ModContext parentContext() {
        return this.modContext;
    }
}
