package org.cloudburstmc.api.configuration;

import org.cloudburstmc.api.util.Identifier;

import java.util.Map;

/**
 * Explicit value codecs shared by a configuration and its loader.
 * Registrations are local to this registry and require external synchronization.
 */
public interface ConfigurationCodecRegistry {

    /**
     * Registers a codec under a unique identifier and value class.
     * Duplicate identifiers or classes are rejected.
     *
     * @param id the identifier stored with encoded values
     * @param type the class restored by the codec
     * @param codec the encoder and decoder
     * @param <T> the represented value type
     * @throws IllegalArgumentException if the identifier or class is already registered
     */
    <T> void registerCodec(Identifier id, Class<T> type, ConfigurationCodec<T> codec);

    /**
     * Encodes a value using the most specific registered class.
     * Unrelated matching types are rejected as ambiguous.
     *
     * @param value the value to encode
     * @return an independent mapping suitable for configuration storage
     * @throws IllegalArgumentException if no unique matching codec exists
     */
    Map<String, Object> encode(Object value);

    /**
     * Decodes a value using its registered identifier and expected class.
     *
     * @param value a mapping returned by {@link #encode(Object)} or read from saved configuration
     * @param type the registered class expected by the caller
     * @param <T> the decoded value type
     * @return the decoded value
     * @throws IllegalArgumentException if the identifier, expected class, or data is invalid
     */
    <T> T decode(Map<String, Object> value, Class<T> type);

    /**
     * Tests whether a mapping identifies a codec for the requested class.
     * This does not validate or decode the mapping's data.
     *
     * @param value the value to inspect
     * @param type the registered class to match
     * @return whether the value identifies that class's codec
     */
    boolean isEncoded(Object value, Class<?> type);
}
