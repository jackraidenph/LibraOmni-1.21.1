package dev.jackraidenph.libraomni.runtime;

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

    default void listenToBus(IEventBus eventBus) {

    }

    enum LifecycleStage {
        CONSTRUCT,
        COMMON,
        CLIENT
    }
}