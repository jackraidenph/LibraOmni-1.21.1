package dev.jackraidenph.libraomni.context;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.run.RuntimeProcessorsManager;
import dev.jackraidenph.libraomni.annotation.run.api.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.annotation.run.impl.RegisterAnnotationProcessor;
import dev.jackraidenph.libraomni.context.extension.api.ModContextExtension;
import dev.jackraidenph.libraomni.context.extension.impl.RegistrationContextExtension;
import net.neoforged.fml.ModContainer;

import java.util.HashSet;
import java.util.Set;

public class ModContext implements AutoCloseable {

    private final ModContainer modContainer;
    private boolean closed = false;

    private final Set<ModContextExtension> extensions = new HashSet<>();

    private final RegistrationContextExtension registrationContextExtension;

    private final RuntimeProcessorsManager runtimeProcessorsManager;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;

        this.registrationContextExtension = new RegistrationContextExtension(this);
        this.initExtensions();

        this.runtimeProcessorsManager = new RuntimeProcessorsManager(this);
        this.initRunProcessors();
    }

    private void initExtensions() {
        this.addExtension(this.registrationContextExtension);
    }

    private void initRunProcessors() {
        this.runtimeProcessorsManager.registerProcessor(RegisterAnnotationProcessor.INSTANCE);
    }

    public RegistrationContextExtension getRegisterHandler() {
        return registrationContextExtension;
    }

    private void addExtension(ModContextExtension handler) {
        this.extensions.add(handler);
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public boolean isClosed() {
        return closed;
    }

    public void invokeConstruct() {
        for (ModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing construct setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onModConstruct();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CONSTRUCT);
    }

    public void invokeCommon() {
        for (ModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing common setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onCommonSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.COMMON);
    }

    public void invokeClient() {
        for (ModContextExtension extension : this.extensions) {
            LibraOmni.LOGGER.info("Performing client setup of {} for {}...",
                    extension.getClass().getSimpleName(),
                    this.modContainer.getModId()
            );
            extension.onClientSetup();
        }
        this.runtimeProcessorsManager.onProcess(Scope.CLIENT);
    }

    private void onClose() {
        for (ModContextExtension extension : this.extensions) {
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
}
