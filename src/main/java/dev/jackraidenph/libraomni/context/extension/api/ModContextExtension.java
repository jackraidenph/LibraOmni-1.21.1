package dev.jackraidenph.libraomni.context.extension.api;

public interface ModContextExtension {

    void onCommonSetup();

    void onClientSetup();

    void onModConstruct();

    void onClose();
}
