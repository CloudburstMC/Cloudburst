package org.cloudburstmc.api.item;

import org.cloudburstmc.api.util.Identifier;

/**
 * A smithing trim material, pairing the material's identifier and display color
 * with the item that represents it.
 */
public record TrimMaterial(String materialId, BedrockNamedColor color, Identifier itemId) {
}
