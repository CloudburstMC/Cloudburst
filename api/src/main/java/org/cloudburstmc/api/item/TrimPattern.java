package org.cloudburstmc.api.item;

import org.cloudburstmc.api.util.Identifier;

/**
 * A smithing trim pattern, pairing the pattern's identifier with the smithing
 * template item that unlocks it.
 */
public record TrimPattern(Identifier itemId, String patternId) {
}
