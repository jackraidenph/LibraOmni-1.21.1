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

    private final Set<AbstractModContextExtension> extensions = new HashSet<>();

    public final Registration REGISTRATION;

    private final RuntimeProcessorsManager runtimeProcessorsManager;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;

        this.addExtensions(
                this.REGISTRATION = new Registration()
        );

        this.initRunProcessors(
                this.runtimeProcessorsManager = new RuntimeProcessorsManager(this),

                RegisterAnnotationProcessor.INSTANCE
        );
    }

    private void initRunProcessors(RuntimeProcessorsManager runtimeProcessorsManager, RuntimeProcessor... processors) {
        for (RuntimeProcessor runtimeProcessor : processors) {
            runtimeProcessorsManager.registerProcessor(runtimeProcessor);
        }
    }

    private void addExtensions(AbstractModContextExtension... handler) {
        this.extensions.addAll(List.of(handler));
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public boolean isClosed() {
        return closed;
    }

    public void invokeConstruct() {
        for (AbstractModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing construct setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onModConstruct();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CONSTRUCT);
    }

    public void invokeCommon() {
        for (AbstractModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing common setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onCommonSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.COMMON);
    }

    public void invokeClient() {
        for (AbstractModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing client setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onClientSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CLIENT);
    }

    private void onClose() {
        for (AbstractModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Closing {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onClose();
        }
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

    public class Registration extends AbstractModContextExtension {

        private final Map<ResourceKey<?>, DeferredRegister<?>> registersMap = new HashMap<>();
        private final DeferredRegister.Blocks blocksRegister;
        private final DeferredRegister.Items itemsRegister;

        private Registration() {
            this.blocksRegister = DeferredRegister.createBlocks(this.parentContext().modContainer().getModId());
            this.itemsRegister = DeferredRegister.createItems(this.parentContext().modContainer().getModId());

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

        <T> void addRegister(ResourceKey<T> resourceKey, DeferredRegister<T> register) {
            this.registersMap.put(resourceKey, register);
        }

        public Collection<DeferredRegister<?>> allRegisters() {
            return this.registersMap.values();
        }

        @Override
        void onModConstruct() {
            for (DeferredRegister<?> deferredRegister : this.allRegisters()) {
                IEventBus eventBus = this.parentContext().modContainer().getEventBus();
                if (eventBus != null) {
                    deferredRegister.register(eventBus);
                }
            }
        }
    }

    private abstract class AbstractModContextExtension {

        ModContext parentContext() {
            return ModContext.this;
        }

        void onClose() {
        }

        void onCommonSetup() {
        }

        void onClientSetup() {
        }

        void onModConstruct() {
        }
    }

}
