package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorSand extends BlockBehaviorFallable {


    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
