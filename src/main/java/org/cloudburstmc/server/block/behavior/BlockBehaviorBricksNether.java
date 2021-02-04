package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorBricksNether extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }
}
