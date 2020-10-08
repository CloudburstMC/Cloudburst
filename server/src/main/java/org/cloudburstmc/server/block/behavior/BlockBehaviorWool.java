package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorWool extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block.getState()).getColor();
    }

    public DyeColor getDyeColor(BlockState state) {
        return state.ensureTrait(BlockTraits.COLOR);
    }
}
