package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSlime extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.GRASS_BLOCK_COLOR;
    }
}
