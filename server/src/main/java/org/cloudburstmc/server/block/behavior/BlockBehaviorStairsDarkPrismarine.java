package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsDarkPrismarine extends BlockBehaviorStairsPrismarine {

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }
}
