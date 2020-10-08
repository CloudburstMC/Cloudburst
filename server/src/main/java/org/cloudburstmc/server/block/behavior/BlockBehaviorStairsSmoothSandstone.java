package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsSmoothSandstone extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SAND_BLOCK_COLOR;
    }


}
