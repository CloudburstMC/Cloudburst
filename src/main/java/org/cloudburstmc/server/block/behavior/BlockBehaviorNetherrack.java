package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorNetherrack extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }


}
