package dev.jackraidenph.libraomni.context;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisterAnnotationProcessor;
import dev.jackraidenph.libraomni.context.extension.base.ModContextExtension;
import dev.jackraidenph.libraomni.context.extension.impl.RegistrationContextExtension;
import net.neoforged.fml.ModContainer;

import java.util.HashSet;
import java.util.Set;

public class ModContext implements AutoCloseable {

    private final ModContainer modContainer;
    private boolean closed = false;

    private final Set<ModContextExtension> handlers = new HashSet<>();

    private final RegistrationContextExtension registersCreationHandler;

    private final RuntimeProcessorsManager runtimeProcessorsManager;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;

        this.registersCreationHandler = new RegistrationContextExtension(this);
        this.initHandlers(this.registersCreationHandler);

        this.runtimeProcessorsManager = new RuntimeProcessorsManager(this);
        this.initRunProcessors(this.runtimeProcessorsManager);
    }

    private void initHandlers(RegistrationContextExtension registersCreationHandler) {
        this.addHandler(registersCreationHandler);
    }

    private void initRunProcessors(RuntimeProcessorsManager runtimeProcessorsManager) {
        runtimeProcessorsManager.registerProcessor(new RegisterAnnotationProcessor());
    }

    public RegistrationContextExtension getRegisterHandler() {
        return registersCreationHandler;
    }

    private void addHandler(ModContextExtension handler) {
        this.handlers.add(handler);
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public boolean isClosed() {
        return closed;
    }

    public void invokeConstruct() {
        for (ModContextExtension handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing construct setup of {} for {}...",
                    handler.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            handler.onModConstruct();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CONSTRUCT);
    }

    public void invokeCommon() {
        for (ModContextExtension handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing common setup of {} for {}...",
                    handler.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            handler.onCommonSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.COMMON);
    }

    public void invokeClient() {
        for (ModContextExtension handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing client setup of {} for {}...",
                    handler.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            handler.onClientSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CLIENT);
    }

    private void onClose() {
        for (ModContextExtension handler : this.handlers) {
            LibraOmni.LOGGER.info("Closing {} for {}...",
                    handler.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            handler.onClose();
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
}
