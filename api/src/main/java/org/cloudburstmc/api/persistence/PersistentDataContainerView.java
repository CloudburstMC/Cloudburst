package org.cloudburstmc.api.persistence;

import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only access to custom data. Keys must have a non-empty namespace and path.
 */
public interface PersistentDataContainerView {

    /**
     * Returns whether a value exists at the key, regardless of type.
     *
     * @param key namespaced key
     * @return whether the key exists
     */
    boolean has(Identifier key);

    /**
     * Checks the stored type without decoding the value. Adapters that use the
     * same storage type match the same entry.
     *
     * @param key  namespaced key
     * @param type requested type
     * @return whether the key and type match
     */
    boolean has(Identifier key, PersistentDataType<?, ?> type);

    /**
     * Reads and decodes a value. Array storage is copied before decoding.
     *
     * @param key  namespaced key
     * @param type requested type
     * @param <P>  storage type
     * @param <C>  Java value type
     * @return the decoded value, or empty when missing or of another storage type
     */
    <P, C> Optional<C> get(Identifier key, PersistentDataType<P, C> type);

    /**
     * Reads a value or returns the supplied default when there is no match.
     *
     * @param key          namespaced key
     * @param type         requested type
     * @param defaultValue non-null fallback
     * @param <P>          storage type
     * @param <C>          Java value type
     * @return the stored value or the fallback
     */
    default <P, C> C getOrDefault(Identifier key, PersistentDataType<P, C> type, C defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        return this.get(key, type).orElse(defaultValue);
    }

    /**
     * Returns an immutable snapshot of the stored keys.
     *
     * @return stored keys
     */
    Set<Identifier> getKeys();

    /**
     * Checks whether this view exposes any keys.
     *
     * @return whether no keys are stored
     */
    default boolean isEmpty() {
        return this.getKeys().isEmpty();
    }
}
