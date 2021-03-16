package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorDriedKelp extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.GREEN_BLOCK_COLOR;
    }
}
