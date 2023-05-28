package org.cloudburstmc.api.util.behavior;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.registry.BehaviorRegistry;

public interface BehaviorCollection {

    /**
     * Get behavior for specified key
     *
     * @param key behavior key
     * @param <T> function type
     * @return function
     */
    <T> T get(BehaviorKey<?, T> key);

    /**
     * Extend existing functionality for this key
     *
     * @param key behavior key
     * @param function function to extend
     * @param <T> function type
     * @return this behavior collection
     */
    <T, R> BehaviorCollection extend(BehaviorKey<T, R> key, T function);

    /**
     * Overwrite default functionality for this key
     *
     * @param key behavior key
     * @param function function to overwrite
     * @param <T> function type
     * @return this behavior collection
     */
    <T, R> BehaviorCollection overwrite(BehaviorKey<T, R> key, T function);

    /**
     * Applies all behaviors in a {@link BehaviorBuilder} to the specified collection.
     *
     * @param builder builder
     */
    void apply(BehaviorBuilder builder);

    /**
     * Get the registry that this behavior collection belongs to.
     *
     * @return behavior registry
     */
    BehaviorRegistry<?> getRegistry();
}
