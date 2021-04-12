package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorSlime extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.GRASS_BLOCK_COLOR;
    }
}