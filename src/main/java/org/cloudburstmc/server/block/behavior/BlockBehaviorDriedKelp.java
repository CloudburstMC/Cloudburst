package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorDriedKelp extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.GREEN_BLOCK_COLOR;
    }
}
