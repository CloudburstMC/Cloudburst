package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsPrismarineBricks extends BlockBehaviorStairsPrismarine {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }
}
