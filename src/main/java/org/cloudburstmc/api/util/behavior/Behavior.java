package org.cloudburstmc.api.util.behavior;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.registry.BehaviorRegistry;

public interface Behavior<E> {

    /**
     * Returns the parent function or the default if nothing is set.
     *
     * @return function
     */
    @NonNull
    E parent();

    /**
     * Get a behavior for a specified key
     *
     * @param key behavior key
     * @param <T> function type
     * @return behavior function
     */
    <T> T get(BehaviorKey<?, T> key);

    /**
     * Retrieves a {@link BehaviorRegistry} for the given class.
     *
     * @param type class of registry type
     * @param <T> registry type
     * @return behavior registry
     * @throws IllegalArgumentException if registry type does not exist
     */
    <T> BehaviorRegistry<T> getRegistry(Class<T> type);
}
