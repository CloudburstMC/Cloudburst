package org.cloudburstmc.api.util.behavior;

import org.cloudburstmc.api.data.BehaviorKey;

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
}
