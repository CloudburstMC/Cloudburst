package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSand extends BlockBehaviorFallable {


    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
