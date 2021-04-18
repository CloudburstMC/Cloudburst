package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorStairsDiorite extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }

}
