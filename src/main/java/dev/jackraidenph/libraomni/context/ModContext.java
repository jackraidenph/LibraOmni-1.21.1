package dev.jackraidenph.libraomni.context;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisterAnnotationProcessor;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class ModContext implements AutoCloseable {

    private final ModContainer modContainer;
    private boolean closed = false;

    private final RuntimeProcessorsManager runtimeProcessorsManager;

    private final Map<ResourceKey<?>, DeferredRegister<?>> registersMap;

    private DeferredRegister.Blocks blocksRegister;
    private DeferredRegister.Items itemsRegister;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;
        this.registersMap = new HashMap<>();

        this.initRunProcessors(
                this.runtimeProcessorsManager = new RuntimeProcessorsManager(this),

                RegisterAnnotationProcessor.INSTANCE
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

    public boolean isClosed() {
        return closed;
    }

    public void invokeConstruct() {
        for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
            IEventBus eventBus = this.modContainer().getEventBus();
            if (eventBus != null) {
                deferredRegister.register(eventBus);
            }
        }

        this.runtimeProcessorsManager.onProcess(Scope.CONSTRUCT);
    }

    public void invokeCommon() {
        this.runtimeProcessorsManager.onProcess(Scope.COMMON);
    }

    public void invokeClient() {
        this.runtimeProcessorsManager.onProcess(Scope.CLIENT);
    }

    private void onClose() {
        this.runtimeProcessorsManager.onFinish();
    }

    @Override
    public void close() {
        LibraOmni.LOGGER.info("Closing mod context for {}...", this.modContainer.getModId());
        try {
            this.onClose();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close " + this.modContainer.getModId(), e);
        }

        this.closed = true;

        LibraOmni.LOGGER.info(
                "Mod context for {} was successfully closed", this.modContainer.getModId()
        );
    }
}
