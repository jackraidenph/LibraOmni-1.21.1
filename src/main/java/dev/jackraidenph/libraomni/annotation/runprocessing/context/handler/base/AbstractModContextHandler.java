package dev.jackraidenph.libraomni.annotation.runprocessing.context.handler.base;

import dev.jackraidenph.libraomni.annotation.runprocessing.context.ModContext;

public abstract class AbstractModContextHandler implements ModContextHandler {

    private final ModContext modContext;

    public AbstractModContextHandler(ModContext modContext) {
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
