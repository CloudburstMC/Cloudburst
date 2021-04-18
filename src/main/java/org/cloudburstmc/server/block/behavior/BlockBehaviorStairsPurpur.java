package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorStairsPurpur extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.MAGENTA_BLOCK_COLOR;
    }
}
