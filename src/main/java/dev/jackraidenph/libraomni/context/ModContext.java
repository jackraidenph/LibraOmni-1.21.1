package dev.jackraidenph.libraomni.context;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.context.handler.base.ModContextHandler;
import dev.jackraidenph.libraomni.context.handler.impl.RegisterHandler;
import net.neoforged.fml.ModContainer;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class ModContext implements AutoCloseable {

    private final ModContainer modContainer;
    private boolean closed = false;

    private final Set<ModContextHandler> handlers = new HashSet<>();

    private final RegisterHandler registerHandler;

    public ModContext(ModContainer modContainer) {
        this.modContainer = modContainer;

        this.registerHandler = new RegisterHandler(this);
        this.addHandler(this.registerHandler);
    }

    public RegisterHandler getRegisterHandler() {
        return registerHandler;
    }

    private void addHandler(ModContextHandler handler) {
        this.handlers.add(handler);
    }

    public ModContainer modContainer() {
        return modContainer;
    }

    public boolean isClosed() {
        return closed;
    }

    public void invokeConstruct() {
        for (ModContextHandler handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing construct setup of {} for {}...", handler.getClass().getSimpleName(), this.modContainer.getModId());
            handler.onModConstruct();
        }
    }

    public void invokeCommon() {
        for (ModContextHandler handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing common setup of {} for {}...", handler.getClass().getSimpleName(), this.modContainer.getModId());
            handler.onCommonSetup();
        }
    }

    public void invokeClient() {
        for (ModContextHandler handler : this.handlers) {
            LibraOmni.LOGGER.info("Performing client setup of {} for {}...", handler.getClass().getSimpleName(), this.modContainer.getModId());
            handler.onClientSetup();
        }
    }

    private void onClose() {
        for (ModContextHandler handler : this.handlers) {
            LibraOmni.LOGGER.info("Closing {} for {}...", handler.getClass().getSimpleName(), this.modContainer.getModId());
            handler.onClose();
        }
    }

    @Override
    public void close() {
        LibraOmni.LOGGER.info("Closing mod context for {}...", this.modContainer.getModId());
        try {
            this.onClose();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close handlers for " + this.modContainer.getModId(), e);
        }
        this.closed = true;

        StringJoiner joiner = new StringJoiner(", ");
        for (ModContextHandler handler : this.handlers) {
            joiner.add(handler.getClass().getSimpleName());
        }

        String joinerString = joiner.toString();
        LibraOmni.LOGGER.info(
                "Mod context for {} was successfully closed after closing all handlers {}",
                this.modContainer.getModId(),
                joinerString.isBlank() ? "" : ": " + joinerString
        );
    }
}
