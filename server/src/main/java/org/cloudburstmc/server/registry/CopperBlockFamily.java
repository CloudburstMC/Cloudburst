package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockType;

import java.util.List;

/**
 * Pairs unwaxed and waxed variants in unaffected, exposed, weathered and oxidized order.
 */
public record CopperBlockFamily(List<BlockType> unwaxed, List<BlockType> waxed) {

    public CopperBlockFamily {
        unwaxed = List.copyOf(unwaxed);
        waxed = List.copyOf(waxed);
        if (unwaxed.size() != 4 || waxed.size() != 4) {
            throw new IllegalArgumentException("Copper families must contain four unwaxed and four waxed stages");
        }
    }
}
