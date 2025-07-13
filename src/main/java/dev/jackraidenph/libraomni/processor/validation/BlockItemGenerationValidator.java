package dev.jackraidenph.libraomni.processor.validation;

import org.jetbrains.annotations.NotNull;

public class BlockItemGenerationValidator extends ExtensionValidator {
    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return "net.minecraft.world.level.block.Block";
    }
}
