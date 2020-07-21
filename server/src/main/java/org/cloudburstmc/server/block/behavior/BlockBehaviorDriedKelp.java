package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorDriedKelp extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.GREEN_BLOCK_COLOR;
    }
}
