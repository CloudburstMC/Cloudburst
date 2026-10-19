package org.cloudburstmc.api.configuration;

import java.util.Map;

/**
 * Converts a custom value to and from supported configuration values.
 * Implementations should validate required fields and reject invalid input.
 *
 * @param <T> the represented value type
 */
public interface ConfigurationCodec<T> {

    /**
     * Returns a mapping containing only supported configuration values.
     *
     * @param value the value to encode
     * @return a mapping of strings to values accepted by {@link ConfigurationSection#set(String, Object)}
     * @throws IllegalArgumentException if the value cannot be represented
     */
    Map<String, Object> encode(T value);

    /**
     * Restores a value from its mapping.
     *
     * @param values the fields produced by {@link #encode(Object)}
     * @return the decoded value
     * @throws IllegalArgumentException if the mapping is invalid
     */
    T decode(Map<String, Object> values);
}
