package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockHoneycombBlockBehavior extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }
}
