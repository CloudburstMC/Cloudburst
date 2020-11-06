package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsQuartz extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }

}
