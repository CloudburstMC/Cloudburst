package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsRedSandstone extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }
}