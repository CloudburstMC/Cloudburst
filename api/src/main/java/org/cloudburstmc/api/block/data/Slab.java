package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.SlabType;

/**
 * A bottom, top or double slab.
 */
public interface Slab extends BlockData {

    /**
     * @return the part of the block space occupied by this slab
     */
    SlabType getType();

    /**
     * The resulting block type may change when switching between single and double slabs.
     *
     * @param type the replacement slab type
     * @return the requested slab state, retaining other shared properties
     */
    BlockState withType(SlabType type);
}
