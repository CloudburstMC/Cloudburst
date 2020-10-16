package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLapis extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LAPIS_BLOCK_COLOR;
    }


}
