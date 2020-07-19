package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorDriedKelp extends BlockBehaviorSolid {
    public BlockBehaviorDriedKelp(Identifier id) {
        super(id);
    }

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
