package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class BlockBehaviorTransparent extends BlockBehavior {

    public BlockBehaviorTransparent(Identifier id) {
        super(id);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
