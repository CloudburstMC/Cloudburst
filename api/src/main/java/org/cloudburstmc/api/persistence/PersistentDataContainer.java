package org.cloudburstmc.api.persistence;

import org.cloudburstmc.api.util.Identifier;

/**
 * Stores typed values under namespaced keys independently of built-in data.
 */
public interface PersistentDataContainer extends PersistentDataContainerView {

    /**
     * Stores a value, replacing any previous value at the key. Array storage is
     * copied, so later changes to the array do not affect the stored value.
     *
     * @param key   namespaced key
     * @param type  value type
     * @param value non-null value
     * @param <P>   storage type
     * @param <C>   Java value type
     */
    <P, C> void set(Identifier key, PersistentDataType<P, C> type, C value);

    /**
     * Removes the value at a key, regardless of its type.
     *
     * @param key namespaced key
     */
    void remove(Identifier key);
}
