package dev.jackraidenph.libraomni.context.handler.base;

public interface ModContextHandler {

    void onCommonSetup();

    void onClientSetup();

    void onModConstruct();

    void onClose();
}
