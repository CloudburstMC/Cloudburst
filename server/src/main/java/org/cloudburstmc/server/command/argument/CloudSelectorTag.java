package org.cloudburstmc.server.command.argument;

import java.util.Objects;

/**
 * A required or excluded entity command tag in a target selector.
 *
 * @param value    tag value, or an empty string to test whether the entity has any tags
 * @param inverted whether matching entities must not have the tag
 */
public record CloudSelectorTag(String value, boolean inverted) {

    public CloudSelectorTag {
        Objects.requireNonNull(value, "value");
    }
}
