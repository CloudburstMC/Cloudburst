package org.cloudburstmc.api.plugin;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.configuration.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Manages {@code config.yml} in the plugin's data directory.
 *
 * <p>The bundled {@code config.yml}, when present, supplies defaults for missing
 * values. These defaults are separate from values loaded from disk. Obtain this
 * service through plugin dependency injection and access it from one thread at a time.</p>
 */
public interface PluginConfiguration {

    /**
     * Opens a bundled resource, or returns null when it does not exist.
     * The caller closes the returned stream. Paths are relative to the plugin archive.
     *
     * @param path the nonempty relative resource path
     * @return the resource stream, or null when not bundled
     * @throws IOException if the resource cannot be opened
     * @throws IllegalArgumentException if the path is empty, absolute, or escapes the plugin archive
     */
    @Nullable
    InputStream getResource(String path) throws IOException;

    /**
     * Copies a bundled resource into the plugin's data directory, creating parent directories.
     *
     * @param path    the relative resource path
     * @param replace true to replace an existing file, or false to leave it unchanged
     * @throws IOException              if the resource is missing or cannot be copied
     * @throws IllegalArgumentException if the path escapes the plugin's data directory
     */
    void saveResource(String path, boolean replace) throws IOException;

    /**
     * Returns the current configuration, loading it on first access.
     * A missing file produces an empty configuration with any bundled defaults.
     * This does not create the file.
     *
     * @return the current mutable configuration
     * @throws IOException if the file or bundled defaults cannot be read or parsed
     */
    Configuration getConfig() throws IOException;

    /**
     * Loads a replacement configuration. A failure preserves the current configuration.
     * Unsaved changes are discarded after a successful reload. Previously obtained
     * configuration and child-section references continue to refer to the old values.
     *
     * @throws IOException if the file or bundled defaults cannot be read or parsed
     */
    void reloadConfig() throws IOException;

    /**
     * Saves current explicit values.
     * To save default values as well, first call {@link Configuration#copyDefaults()}.
     * Loads the configuration first if it has not been accessed.
     *
     * @throws IOException if loading, serialization, or writing fails
     */
    void saveConfig() throws IOException;

    /**
     * Copies the bundled config.yml when the file does not exist.
     * Preserves the bundled bytes, including comments. Does not reload a configuration
     * that has already been loaded.
     *
     * @throws IOException if the plugin has no bundled config.yml or copying fails
     */
    void saveDefaultConfig() throws IOException;
}
