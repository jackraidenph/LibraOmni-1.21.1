package dev.jackraidenph.libraomni.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ObjectOriginGetter {
    @Nullable
    String getOriginModId(Object object);

    @Nonnull
    String getObjectName(Object object);
}
