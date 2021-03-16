package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorDaylightDetector extends BlockBehaviorTransparent {


    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    //This function is a suggestion that can be renamed or deleted
    protected boolean invertDetect() {
        return false;
    }


    //todo redstone

}
