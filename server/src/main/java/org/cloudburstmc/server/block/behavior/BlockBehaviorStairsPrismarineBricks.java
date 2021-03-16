package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorStairsPrismarineBricks extends BlockBehaviorStairsPrismarine {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }
}
