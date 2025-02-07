package dev.jackraidenph.libraomni.context;

import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisteredAnnotationProcessor;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class ModContext {

    private final ModContainer modContainer;
    private boolean constructFinished, clientFinished, commonFinished;

    private final RuntimeProcessorsManager runtimeProcessorsManager;

    private final Map<ResourceKey<?>, DeferredRegister<?>> registersMap;

    private DeferredRegister.Blocks blocksRegister;
    private DeferredRegister.Items itemsRegister;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;
        this.registersMap = new HashMap<>();

        this.initRunProcessors(
                this.runtimeProcessorsManager = new RuntimeProcessorsManager(this),

                RegisteredAnnotationProcessor.INSTANCE
        );

        this.initRegistries();
    }

    private void initRegistries() {
        this.blocksRegister = DeferredRegister.createBlocks(modContainer.getModId());
        this.itemsRegister = DeferredRegister.createItems(modContainer.getModId());

        this.registersMap.put(blocksRegister.getRegistryKey(), blocksRegister);
        this.registersMap.put(itemsRegister.getRegistryKey(), itemsRegister);
    }

    public DeferredRegister.Items itemsRegister() {
        return this.itemsRegister;
    }

    public DeferredRegister.Blocks blocksRegister() {
        return this.blocksRegister;
    }

    @SuppressWarnings("unchecked")
    public <T> DeferredRegister<T> getRegister(ResourceKey<T> resourceKey) {
        return (DeferredRegister<T>) registersMap.get(resourceKey);
    }

    private <T> void addRegister(ResourceKey<T> resourceKey, DeferredRegister<T> register) {
        this.registersMap.put(resourceKey, register);
    }

    public Collection<DeferredRegister<?>> allRegisters() {
        return this.registersMap.values();
    }

    private void initRunProcessors(RuntimeProcessorsManager runtimeProcessorsManager, RuntimeProcessor... processors) {
        for (RuntimeProcessor runtimeProcessor : processors) {
            runtimeProcessorsManager.registerProcessor(runtimeProcessor);
        }
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public boolean isFinished() {
        return constructFinished && clientFinished && commonFinished;
    }

    public void invokeConstruct() {
        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
            }
        }

        this.runtimeProcessorsManager.onProcess(Scope.CONSTRUCT);

        this.constructFinished = true;
    }

    public void invokeCommon() {
        this.runtimeProcessorsManager.onProcess(Scope.COMMON);
        this.commonFinished = true;
    }

    public void invokeClient() {
        this.runtimeProcessorsManager.onProcess(Scope.CLIENT);
        this.clientFinished = true;
    }
}
