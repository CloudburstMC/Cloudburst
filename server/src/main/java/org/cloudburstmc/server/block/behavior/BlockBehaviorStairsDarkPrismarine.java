package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsDarkPrismarine extends BlockBehaviorStairsPrismarine {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }
}
