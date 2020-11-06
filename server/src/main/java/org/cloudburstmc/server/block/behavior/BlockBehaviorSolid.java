package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public abstract class BlockBehaviorSolid extends BlockBehavior {

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.STONE_BLOCK_COLOR;
    }
}
