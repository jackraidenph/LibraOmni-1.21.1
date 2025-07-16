package dev.jackraidenph.libraomni.processor.validation;

import org.jetbrains.annotations.NotNull;

public class AddToCreativeTabValidator extends HolderCheckingResolvingAssignabilityValidator {
    private static final String ITEM_LIKE_CLASS = "net.minecraft.world.level.ItemLike";

    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return ITEM_LIKE_CLASS;
    }
}
