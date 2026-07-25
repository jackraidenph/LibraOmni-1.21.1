package dev.jackraidenph.libraomni.gradle;

import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.ConflictPolicy;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public abstract class LibraOmniExtension {

    public static String NAME = "libraOmni";

    public LibraOmniExtension() {
        getBlackMagicEnabled().convention(false);
        getCacheDisabled().convention(false);
    }

    public abstract Property<Boolean> getBlackMagicEnabled();

    public void enableBlackMagic() {
        this.getBlackMagicEnabled().set(true);
    }

    public abstract Property<Boolean> getCacheDisabled();

    public void disableCache() {
        getCacheDisabled().set(true);
    }

    public abstract MapProperty<String, ConflictPolicy> getConflictPolicies();

    public void conflictPolicy(String pattern, ConflictPolicy policy) {
        getConflictPolicies().put(pattern, policy);
    }
}
