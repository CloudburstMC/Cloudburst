package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;

import java.util.Objects;

/**
 * The single and double forms of a slab material.
 *
 * @param single  the bottom and top slab type
 * @param doubled the full-block slab type
 */
public record SlabFamily(BlockType single, BlockType doubled) {

    public SlabFamily {
        Objects.requireNonNull(single, "single");
        Objects.requireNonNull(doubled, "doubled");
        if (single == doubled || !single.getTraits().contains(BlockTraits.SLAB_SLOT) || !doubled.getTraits().contains(BlockTraits.SLAB_SLOT)) {
            throw new IllegalArgumentException("A slab family requires distinct slab forms");
        }
    }
}
