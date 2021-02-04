package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public abstract class BlockBehaviorSolid extends BlockBehavior {

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.STONE_BLOCK_COLOR;
    }
}
