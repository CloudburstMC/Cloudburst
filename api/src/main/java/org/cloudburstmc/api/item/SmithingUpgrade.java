package org.cloudburstmc.api.item;

import org.cloudburstmc.api.util.Identifier;

/**
 * A smithing upgrade pairing, mapping a base item to its upgraded result.
 *
 * @param id     the unique identifier for this upgrade entry
 * @param base   the identifier of the item used as the base input
 * @param result the identifier of the item produced as the output
 */
public record SmithingUpgrade(Identifier id, Identifier base, Identifier result) {
}
