package dev.jackraidenph.libraomni.reflect;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

public interface LifecycleSetup {
    default void setupConstruct(FMLConstructModEvent event) {

    }

    default void setupCommon(FMLCommonSetupEvent event) {

    }

    default void setupClient(FMLClientSetupEvent event) {

    }

    default void subscribeAll(IEventBus eventBus) {
        eventBus.addListener(this::setupConstruct);
        eventBus.addListener(this::setupCommon);
        eventBus.addListener(this::setupClient);
    }

    enum LifecycleStage {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}