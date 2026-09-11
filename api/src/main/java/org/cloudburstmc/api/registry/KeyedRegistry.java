package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registry whose entries are addressed by stable identifiers.
 *
 * @param <T> registered value type
 */
public interface KeyedRegistry<T> extends Registry {

    /**
     * Finds an entry by identifier.
     *
     * @param id the entry identifier
     * @return the matching entry, if registered
     */
    Optional<T> get(Identifier id);

    /**
     * Returns the identifier for a registered entry.
     *
     * @param value the registered entry
     * @return the entry identifier
     * @throws IllegalArgumentException if the entry is not registered
     */
    Identifier getId(T value);

    /**
     * Returns all registered entries.
     *
     * @return immutable collection of registered entries
     */
    Collection<T> values();

    /**
     * Returns whether an identifier is registered.
     *
     * @param id the entry identifier
     * @return whether the identifier has a matching entry
     */
    default boolean contains(Identifier id) {
        return this.get(Objects.requireNonNull(id, "id")).isPresent();
    }

    /**
     * Gets an entry or throws if no entry is registered for the identifier.
     *
     * @param id the entry identifier
     * @return the matching entry
     * @throws NoSuchElementException if no entry is registered for the identifier
     */
    default T getOrThrow(Identifier id) {
        return this.get(Objects.requireNonNull(id, "id"))
                .orElseThrow(() -> new NoSuchElementException("No registry entry for " + id));
    }

    /**
     * Returns a stream of all registered entries.
     *
     * @return entry stream
     */
    default Stream<T> stream() {
        return this.values().stream();
    }

    /**
     * Returns a stream of all registered identifiers.
     *
     * @return identifier stream
     */
    default Stream<Identifier> keyStream() {
        return this.values().stream().map(this::getId);
    }
}
