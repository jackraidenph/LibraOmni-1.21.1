package dev.jackraidenph.libraomni.compilation.validation;

import org.jetbrains.annotations.NotNull;

public class AddToCreativeTabValidator extends HolderOrTypeValidator {
    private static final String ITEM_LIKE_CLASS = "net.minecraft.world.level.ItemLike";

    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return ITEM_LIKE_CLASS;
    }
}
