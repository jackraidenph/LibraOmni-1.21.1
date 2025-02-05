package dev.jackraidenph.libraomni.context.extension.api;

import dev.jackraidenph.libraomni.context.ModContext;

public interface ModContextExtension {

    void onCommonSetup();

    void onClientSetup();

    void onModConstruct();

    void onClose();

    ModContext parentContext();
}
