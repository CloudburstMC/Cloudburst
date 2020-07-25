package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;

public abstract class BlockBehaviorSolid extends BlockBehavior {

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.STONE_BLOCK_COLOR;
    }
}
