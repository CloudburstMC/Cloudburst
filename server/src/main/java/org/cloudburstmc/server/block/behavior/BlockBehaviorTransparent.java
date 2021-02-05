package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public abstract class BlockBehaviorTransparent extends BaseBlockBehavior {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
