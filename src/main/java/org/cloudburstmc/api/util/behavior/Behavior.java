package org.cloudburstmc.api.util.behavior;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.data.DataKey;

public interface Behavior {

    /**
     * Returns the parent function or the default if nothing is set.
     *
     * @param <T> function type
     * @return function
     */
    @NonNull
    <T> T getSuper();

    /**
     * Get a behavior for a specified key
     *
     * @param key behavior key
     * @param <T> function type
     * @return behavior function
     */
    <T> T getBehavior(DataKey<?, T> key);
}
