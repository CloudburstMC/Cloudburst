package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;

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