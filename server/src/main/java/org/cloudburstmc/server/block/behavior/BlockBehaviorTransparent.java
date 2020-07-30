package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public abstract class BlockBehaviorTransparent extends BlockBehavior {

    public BlockBehaviorTransparent() {
        this(null);
    }

    public BlockBehaviorTransparent(Identifier type) {
        super(type);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.TRANSPARENT_BLOCK_COLOR;
    }

}
