package org.cloudburstmc.api.persistence;

import org.cloudburstmc.api.util.Identifier;

import java.util.Optional;
import java.util.Set;

/**
 * Stores typed values under namespaced keys independently of built-in data.
 */
public interface PersistentDataContainer {

    /**
     * Stores a value, replacing any previous value at the key.
     *
     * @param key   namespaced key
     * @param type  value type
     * @param value non-null value
     * @param <T>   Java value type
     */
    <T> void set(Identifier key, PersistentDataType<T> type, T value);

    /**
     * Reads a value if the key exists and was stored with the requested type.
     *
     * @param key  namespaced key
     * @param type requested type
     * @param <T>  Java value type
     * @return the stored value, or empty when missing or of another type
     */
    <T> Optional<T> get(Identifier key, PersistentDataType<T> type);

    /**
     * Removes the value at a key, regardless of its type.
     *
     * @param key namespaced key
     */
    void remove(Identifier key);

    /**
     * Returns an immutable snapshot of the stored keys.
     *
     * @return stored keys
     */
    Set<Identifier> getKeys();
}
