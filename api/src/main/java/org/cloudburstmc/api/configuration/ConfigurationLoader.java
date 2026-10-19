package org.cloudburstmc.api.configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

/**
 * Creates and reads YAML configurations. Available through plugin dependency injection.
 *
 * <p>Documents must contain a mapping. Scalar keys are read as literal strings, including
 * words such as {@code on} and numeric keys. Collection keys, duplicate keys, and unsupported
 * values are rejected. Empty documents produce empty configurations. Loading does
 * not write files or copy defaults.</p>
 *
 * <p>Loaders provide codecs for identifiers, vectors, block states, items, colors, player references, and locations.
 * Custom item component values that cannot be saved directly require a registered value codec.
 * Unsupported values are rejected rather than discarded.
 * Location decoding requires the saved level to be available. Plugin-injected loaders
 * resolve currently loaded levels and never load levels as a side effect.
 * Encoded values are ordinary mappings. Loading YAML never instantiates custom classes.
 * Use typed section accessors or {@link #decode(java.util.Map, Class)} to restore a registered value.</p>
 */
public interface ConfigurationLoader extends ConfigurationCodecRegistry {

    /**
     * Creates an independent loader with a snapshot of the supplied YAML options.
     * Existing codecs are copied. Later registrations on either loader do not affect the other.
     *
     * @param options formatting and document limits for the new loader
     * @return the new loader
     */
    ConfigurationLoader withOptions(YamlConfigurationOptions options);

    /**
     * Creates an empty configuration.
     *
     * @return a new configuration with no values or defaults
     */
    Configuration create();

    /**
     * Reads UTF-8 YAML. A missing file produces an empty configuration without creating a file.
     * The configured document size limit is enforced while reading.
     *
     * @param path the file to read
     * @return a new configuration containing the file's values and comments
     * @throws IOException if the file cannot be read or its contents are invalid
     */
    Configuration load(Path path) throws IOException;

    /**
     * Reads YAML without closing the supplied reader.
     * The configured document size limit is enforced while reading.
     *
     * @param reader the document source
     * @return a new configuration containing the document's values and comments
     * @throws IOException if reading fails or the document is invalid
     */
    Configuration load(Reader reader) throws IOException;

    /**
     * Parses a mapping document.
     *
     * @param yaml the YAML document
     * @return a new configuration containing the document's values and comments
     * @throws IOException if the document is invalid
     */
    Configuration parse(String yaml) throws IOException;

    /**
     * Serializes explicit values as YAML.
     * Defaults are included only after {@link Configuration#copyDefaults()}.
     * Formatting and quoting may change when a document is serialized.
     *
     * @param configuration a configuration created by this loader or a loader returned by {@link #withOptions}
     * @return the YAML document
     * @throws IOException              if serialization fails
     * @throws IllegalArgumentException if the configuration implementation is unsupported
     */
    String serialize(Configuration configuration) throws IOException;

    /**
     * Saves explicit values and comments as UTF-8 YAML, creating parent directories.
     * Replaces an existing file only after serialization and writing have succeeded.
     * Replacement is atomic when supported by the file system.
     *
     * @param configuration a configuration created by this loader or a loader returned by {@link #withOptions}
     * @param path          the destination file
     * @throws IOException              if serialization or writing fails
     * @throws IllegalArgumentException if the configuration implementation is unsupported
     */
    void save(Configuration configuration, Path path) throws IOException;
}
