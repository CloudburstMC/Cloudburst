package org.cloudburstmc.api.configuration;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A root configuration with defaults stored separately from explicit values.
 */
public interface Configuration extends ConfigurationSection {

    /**
     * Returns the codecs used by typed accessors on this configuration and its sections.
     * Register custom value codecs here before reading their saved values.
     */
    ConfigurationCodecRegistry getCodecs();

    /**
     * Returns the defaults provider, or null when none has been assigned.
     */
    @Nullable
    Configuration getDefaults();

    /**
     * Sets the live configuration supplying defaults. Later changes to it affect reads here.
     * Default values and their path comments are saved only after {@link #copyDefaults()}.
     *
     * @param defaults the configuration supplying missing values
     * @throws IllegalArgumentException if assigning it would create a cycle of defaults
     */
    void setDefaults(Configuration defaults);

    /**
     * Adds or replaces a default. Null removes it.
     */
    void addDefault(String path, @Nullable Object value);

    /**
     * Copies values at the supplied paths into the defaults without replacing unrelated defaults.
     */
    default void addDefaults(Map<String, ?> values) {
        values.forEach(this::addDefault);
    }

    /**
     * Copies explicit leaf values from another configuration into the defaults.
     * Later changes to the source are not reflected here.
     */
    default void addDefaults(Configuration source) {
        for (String path : source.getKeys(true)) {
            if (!source.isConfigurationSection(path)) {
                addDefault(path, source.get(path));
            }
        }
    }

    /**
     * Copies missing defaults into explicit values, recursively.
     * Existing explicit values are preserved. Copied values are included in subsequent saves.
     * Includes chained defaults and preserves existing live section references.
     */
    void copyDefaults();

    /**
     * Returns an unmodifiable snapshot of document footer comments.
     * Null entries represent blank lines.
     */
    List<String> getFooter();

    /**
     * Replaces document footer comments. Null entries represent blank lines.
     * Supply an empty list to clear them.
     */
    void setFooter(List<String> comments);
}
