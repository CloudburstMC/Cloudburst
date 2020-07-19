package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class BlockBehaviorSolid extends BlockBehavior {

    public BlockBehaviorSolid(Identifier id) {
        super(id);
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.STONE_BLOCK_COLOR;
    }
}
