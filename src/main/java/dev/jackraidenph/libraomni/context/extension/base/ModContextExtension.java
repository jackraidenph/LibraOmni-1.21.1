package dev.jackraidenph.libraomni.context.extension.base;

public interface ModContextExtension {

    void onCommonSetup();

    void onClientSetup();

    void onModConstruct();

    void onClose();
}
