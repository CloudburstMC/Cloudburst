package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorFenceWooden extends BlockBehaviorFence {


    @Override
    public boolean canConnect(BlockState state) {
        return (state.inCategory(BlockCategory.FENCE) || state.inCategory(BlockCategory.FENCE_GATE)) || state.inCategory(BlockCategory.SOLID) && !state.inCategory(BlockCategory.TRANSPARENT);
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
