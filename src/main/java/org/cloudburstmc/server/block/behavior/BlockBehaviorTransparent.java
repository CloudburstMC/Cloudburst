package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class BlockBehaviorTransparent extends BlockBehavior {

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
