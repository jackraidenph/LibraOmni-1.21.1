package dev.jackraidenph.libraomni.compilation.validation;

import org.jetbrains.annotations.NotNull;

public class BlockItemGenerationValidator extends HolderOrTypeValidator {
    private static final String BLOCK_CLASS = "net.minecraft.world.level.block.Block";

    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return BLOCK_CLASS;
    }
}
