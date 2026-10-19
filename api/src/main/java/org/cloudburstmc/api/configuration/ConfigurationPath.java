package org.cloudburstmc.api.configuration;

import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds configuration paths from literal keys without interpreting dots or brackets.
 */
@UtilityClass
public class ConfigurationPath {

    /**
     * Escapes one nonempty literal key for use in a value or comment path.
     *
     * @param key the literal key, not an already escaped path
     * @return the escaped key
     * @throws IllegalArgumentException if the key is empty
     */
    public static String key(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Configuration keys must not be empty");
        }

        return key.replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    /**
     * Joins literal keys into a relative path. With no keys, returns the empty section path.
     *
     * @param keys the literal keys in parent-to-child order
     * @return the escaped, dot-separated path
     * @throws IllegalArgumentException if any key is empty
     */
    public static String of(String... keys) {
        Objects.requireNonNull(keys, "keys");
        StringJoiner path = new StringJoiner(".");

        for (String key : keys) {
            path.add(key(key));
        }

        return path.toString();
    }
}
