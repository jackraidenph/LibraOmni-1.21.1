package dev.jackraidenph.libraomni.common;

import javax.annotation.Nullable;

public interface ObjectOriginGetter {
    @Nullable
    String getOriginModId(Object object);
}
