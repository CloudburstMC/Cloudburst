package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public abstract class BlockBehaviorTransparent extends BlockBehavior {

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
