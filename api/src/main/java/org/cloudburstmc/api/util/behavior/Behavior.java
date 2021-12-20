package org.cloudburstmc.api.util.behavior;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.data.BehaviorKey;

public interface Behavior<T> {

    /**
     * Returns the parent function or the default if nothing is set.
     *
     * @return function
     */
    @NonNull
    T parent();

    /**
     * Get a behavior for a specified key
     *
     * @param key behavior key
     * @param <U> function type
     * @return behavior function
     */
    <U> U get(BehaviorKey<?, U> key);
}
