package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedstone extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.REDSTONE_BLOCK_COLOR;
    }


    @Override
    public int getWeakPower(Block block, Direction face) {
        return 15;
    }


}