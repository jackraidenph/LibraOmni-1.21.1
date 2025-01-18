package dev.jackraidenph.libraomni.annotation.runprocessing.context.handler.base;

public interface ModContextHandler {

    void onCommonSetup();

    void onClientSetup();

    void onModConstruct();

    void onClose();
}
