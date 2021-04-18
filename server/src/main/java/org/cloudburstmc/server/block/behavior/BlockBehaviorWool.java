package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;

public class BlockBehaviorWool extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block.getState()).getColor();
    }

    public DyeColor getDyeColor(BlockState state) {
        return state.ensureTrait(BlockTraits.COLOR);
    }
}
