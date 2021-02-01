package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTraits;

import static org.cloudburstmc.api.block.BlockTypes.WOODEN_SLAB;

public class BlockBehaviorDoubleSlabWood extends BlockBehaviorDoubleSlab {

    public BlockBehaviorDoubleSlabWood() {
        super(WOODEN_SLAB, BlockTraits.TREE_SPECIES);
    }


}
